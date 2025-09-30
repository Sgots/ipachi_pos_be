package com.ipachi.pos.subscriptions;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPlanSeeder {
    private final SubscriptionPlanRepository repo;

    public SubscriptionPlanSeeder(SubscriptionPlanRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void seed() {
        upsert(SubscriptionTier.BRONZE,   1,   5);
        upsert(SubscriptionTier.SILVER,   2,  15);
        upsert(SubscriptionTier.GOLD,     5,  50);
        upsert(SubscriptionTier.PLATINUM,10, null); // null => unlimited QR codes
    }

    private void upsert(SubscriptionTier tier, int users, Integer qrLimit) {
        var plan = repo.findByTier(tier).orElseGet(SubscriptionPlan::new);
        plan.setTier(tier);
        plan.setUsersAllowed(users);
        plan.setQrCodeLimit(qrLimit);
        repo.save(plan);
    }
}
