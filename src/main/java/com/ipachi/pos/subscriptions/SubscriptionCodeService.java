package com.ipachi.pos.subscriptions;

import com.ipachi.pos.subscriptions.dto.ActivateCodeRequest;
import com.ipachi.pos.subscriptions.dto.BusinessSubscriptionView;
import com.ipachi.pos.subscriptions.dto.EffectivePlanView;
import com.ipachi.pos.subscriptions.dto.GenerateCodesRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubscriptionCodeService {
    private final SubscriptionCodeRepository repo;
    private final SubscriptionPlanRepository planRepo;
    private final BusinessSubscriptionRepository bsubRepo;
    private final BusinessTrialRepository trialRepo;
    private final SecureRandom rng = new SecureRandom();

    public SubscriptionCodeService(
            SubscriptionCodeRepository repo,
            SubscriptionPlanRepository planRepo,
            BusinessSubscriptionRepository bsubRepo,
            BusinessTrialRepository trialRepo
    ) {
        this.repo = repo;
        this.planRepo = planRepo;
        this.bsubRepo = bsubRepo;
        this.trialRepo = trialRepo;
    }

    public Page<SubscriptionCode> search(SubscriptionTier tier, Boolean used, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return repo.search(tier, used, from, to, pageable);
    }

    @Transactional
    public List<SubscriptionCode> generate(GenerateCodesRequest req) {
        if (req.count <= 0 || req.count > 10_000) throw new IllegalArgumentException("count must be 1..10000");
        var now = OffsetDateTime.now();
        var prefix = Optional.ofNullable(req.prefix).orElse("");
        List<SubscriptionCode> batch = new ArrayList<>(req.count);

        // Keep trying until we have req.count unique codes
        while (batch.size() < req.count) {
            String code = prefix + randomCode(12);
            if (repo.findByCode(code).isPresent() || batch.stream().anyMatch(c -> c.getCode().equals(code))) {
                continue;
            }
            SubscriptionCode c = new SubscriptionCode();
            c.setCode(code);
            c.setTier(req.tier);
            c.setGeneratedAt(now);
            c.setExpiresAt(req.expiresAt);
            batch.add(c);
        }
        return repo.saveAll(batch);
    }

    private String randomCode(int len) {
        final String alphabet = "ABCDEFGHJKLMNPQRTUVWXYZ23456789"; // avoid ambiguous chars
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
        }
        // group as XXXX-XXXX-XXXX
        String raw = sb.toString();
        return raw.substring(0,4) + "-" + raw.substring(4,8) + "-" + raw.substring(8,12);
    }

    @Transactional
    public SubscriptionCode activate(ActivateCodeRequest req) {
        var now = OffsetDateTime.now();

        var code = repo.findByCode(Objects.requireNonNull(req.code, "code is required"))
                .orElseThrow(() -> new NoSuchElementException("Invalid code"));

        if (code.getCancelledAt() != null) throw new IllegalStateException("Code has been cancelled");
        if (code.getUsedAt() != null) throw new IllegalStateException("Code already used");
        if (code.getExpiresAt() != null && code.getExpiresAt().isBefore(now))
            throw new IllegalStateException("Code expired");

        Long businessId = Objects.requireNonNull(req.businessId, "businessId is required");

        // If no expiry on the code, make it 1 month from activation
        var effectiveExpiry = code.getExpiresAt();
        if (effectiveExpiry == null) {
            effectiveExpiry = now.plusMonths(1);
            code.setExpiresAt(effectiveExpiry);
        }

        // Mark code used
        code.setUsedAt(now);
        code.setRedeemedByBusinessId(businessId);
        repo.save(code);

        // Apply plan to business
        var plan = planRepo.findByTier(code.getTier())
                .orElseThrow(() -> new IllegalStateException("Plan not configured for tier " + code.getTier()));

        var bsub = bsubRepo.findByBusinessId(businessId).orElseGet(BusinessSubscription::new);
        bsub.setBusinessId(businessId);
        bsub.setTier(code.getTier());
        bsub.setUsersAllowed(plan.getUsersAllowed());
        bsub.setQrCodeLimit(plan.getQrCodeLimit());
        bsub.setActivatedAt(now);
        bsub.setExpiresAt(effectiveExpiry);
        bsubRepo.save(bsub);

        return code;
    }
    @Transactional
    public SubscriptionCode cancelCode(Long codeId, Long adminUserId, String reason) {
        var code = repo.findById(Objects.requireNonNull(codeId, "codeId is required"))
                .orElseThrow(() -> new NoSuchElementException("Code not found"));

        if (code.getUsedAt() != null)
            throw new IllegalStateException("Code already used; terminate the subscription instead.");

        if (code.getCancelledAt() == null) {
            code.setCancelledAt(OffsetDateTime.now());
            code.setCancelledByUserId(adminUserId);
            code.setCancelledReason(reason);
            repo.save(code);
        }
        return code; // idempotent
    }

    @Transactional
    public BusinessSubscription terminateSubscriptionByCode(Long codeId, Long adminUserId, String reason) {
        var code = repo.findById(Objects.requireNonNull(codeId, "codeId is required"))
                .orElseThrow(() -> new NoSuchElementException("Code not found"));

        if (code.getUsedAt() == null)
            throw new IllegalStateException("Code not used; cancel the code instead.");

        var bid = code.getRedeemedByBusinessId();
        if (bid == null) throw new IllegalStateException("No business linked to this code.");

        var bsub = bsubRepo.findByBusinessId(bid)
                .orElseThrow(() -> new NoSuchElementException("No active subscription for this business"));

        // Terminate immediately
        bsub.setExpiresAt(OffsetDateTime.now());
        return bsubRepo.save(bsub);
    }

    @Transactional
    public void cancelTrial(Long businessId) {
        var t = trialRepo.findByBusinessId(Objects.requireNonNull(businessId, "businessId is required"))
                .orElseThrow(() -> new NoSuchElementException("No trial found"));
        if (t.getStatus() == TrialStatus.ACTIVE) {
            t.setStatus(TrialStatus.CANCELLED);
            trialRepo.save(t);
        }
    }
    @Transactional
    public BusinessTrial startFreeTrial(Long businessId, Long activatedByUserId, String ip) {
        Objects.requireNonNull(businessId, "businessId is required");

        var existing = trialRepo.findByBusinessId(businessId);
        if (existing.isPresent()) {
            var t = existing.get();
            if (t.getStatus() == TrialStatus.ACTIVE && t.getEndsAt().isAfter(OffsetDateTime.now())) {
                return t; // idempotent return
            }
            // once-ever policy
            throw new IllegalStateException("Free trial already used for this business.");
        }

        // Optional: block if already on Platinum paid plan
        var paid = bsubRepo.findByBusinessId(businessId).orElse(null);
        if (paid != null && paid.getTier() == SubscriptionTier.PLATINUM
                && (paid.getExpiresAt() == null || paid.getExpiresAt().isAfter(OffsetDateTime.now()))) {
            throw new IllegalStateException("Business already on Platinum plan.");
        }

        var now = OffsetDateTime.now();
        var trial = new BusinessTrial();
        trial.setBusinessId(businessId);
        trial.setTier(SubscriptionTier.PLATINUM);
        trial.setStatus(TrialStatus.ACTIVE);
        trial.setStartedAt(now);
        trial.setEndsAt(now.plusDays(7));
        trial.setActivatedByUserId(activatedByUserId);
        trial.setActivatedFromIp(ip);
        return trialRepo.save(trial);
    }
    /** Expire any trials that have passed their end time (can be run by a scheduler). */
    @Transactional
    public int expireOverdueTrials() {
        var now = OffsetDateTime.now();
        var overdue = trialRepo.findByStatusAndEndsAtBefore(TrialStatus.ACTIVE, now);
        for (var t : overdue) {
            t.setStatus(TrialStatus.EXPIRED);
        }
        trialRepo.saveAll(overdue);
        return overdue.size();
    }

    /** Returns the effective plan (trial takes precedence if active). */
    @Transactional
    public EffectivePlanView getEffectivePlan(Long businessId) {
        var now = OffsetDateTime.now();

        // 1) Check active trial overlay
        var trialOpt = trialRepo.findByBusinessId(businessId);
        if (trialOpt.isPresent()) {
            var t = trialOpt.get();
            if (t.getStatus() == TrialStatus.ACTIVE && t.getEndsAt().isAfter(now)) {
                var plan = planRepo.findByTier(SubscriptionTier.PLATINUM)
                        .orElseThrow(() -> new IllegalStateException("Plan not configured: PLATINUM"));

                var v = new EffectivePlanView();
                v.businessId = businessId;
                v.source = "TRIAL";
                v.tier = SubscriptionTier.PLATINUM;
                v.usersAllowed = plan.getUsersAllowed();
                v.qrCodeLimit = plan.getQrCodeLimit(); // null => unlimited
                v.trialEndsAt = t.getEndsAt();
                v.subscriptionExpiresAt = null;
                return v;
            }
        }

        // 2) Fallback to normal subscription (if any)
        var sub = bsubRepo.findByBusinessId(businessId).orElse(null);
        if (sub != null) {
            var v = new EffectivePlanView();
            v.businessId = businessId;
            v.source = "SUBSCRIPTION";
            v.tier = sub.getTier();
            v.usersAllowed = sub.getUsersAllowed();
            v.qrCodeLimit = sub.getQrCodeLimit();
            v.trialEndsAt = null;
            v.subscriptionExpiresAt = sub.getExpiresAt();
            return v;
        }

        // 3) None → choose a default posture. Here, default to BRONZE limits.
        var bronze = planRepo.findByTier(SubscriptionTier.BRONZE)
                .orElseThrow(() -> new IllegalStateException("Plan not configured: BRONZE"));
        var v = new EffectivePlanView();
        v.businessId = businessId;
        v.source = "NONE";
        v.tier = SubscriptionTier.BRONZE;
        v.usersAllowed = bronze.getUsersAllowed();
        v.qrCodeLimit = bronze.getQrCodeLimit();
        v.trialEndsAt = null;
        v.subscriptionExpiresAt = null;
        return v;
    }

    // already present earlier; exposing to controller for /business/{id}/plan
    public BusinessSubscriptionView serviceGetBusinessPlan(Long businessId) {
        return bsubRepo.findByBusinessId(businessId)
                .map(com.ipachi.pos.subscriptions.dto.BusinessSubscriptionView::of)
                .orElse(null);
    }

}
