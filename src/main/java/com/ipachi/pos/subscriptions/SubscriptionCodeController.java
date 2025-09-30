package com.ipachi.pos.subscriptions;

import com.ipachi.pos.subscriptions.dto.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionCodeController {
    private final SubscriptionCodeService service;
    private final BusinessSubscriptionRepository businessSubscriptionRepository;
    public SubscriptionCodeController(SubscriptionCodeService service, BusinessSubscriptionRepository businessSubscriptionRepository) {
        this.businessSubscriptionRepository = businessSubscriptionRepository;
        this.service = service;
    }

    // Admin: list codes (filterable)
    @GetMapping("/codes")
    public Page<SubscriptionCodeView> list(
            @RequestParam(required = false) SubscriptionTier tier,
            @RequestParam(required = false) Boolean used,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        var p = PageRequest.of(page, Math.min(size, 200));
        return service.search(tier, used, from, to, p).map(SubscriptionCodeView::of);
    }

    // Admin: generate new codes
    @PostMapping("/codes/generate")
    public List<SubscriptionCodeView> generate(@RequestBody GenerateCodesRequest req) {
        return service.generate(req).stream().map(SubscriptionCodeView::of).toList();
    }

    // Admin: export CSV (all or filtered)
    @GetMapping("/codes/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) SubscriptionTier tier,
            @RequestParam(required = false) Boolean used
    ) {
        var all = service.search(tier, used, null, null, PageRequest.of(0, 10_000)).getContent();

        var out = new ByteArrayOutputStream();
        // header
        write(out, "id,code,tier,generatedAt,expiresAt,usedAt,redeemedByBusinessId\n");
        for (var c : all) {
            write(out, String.format("%d,%s,%s,%s,%s,%s,%s\n",
                    c.getId(),
                    c.getCode(),
                    c.getTier(),
                    n(c.getGeneratedAt()),
                    n(c.getExpiresAt()),
                    n(c.getUsedAt()),
                    n(c.getRedeemedByBusinessId())));
        }
        var bytes = out.toByteArray();
        var headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subscription-codes.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
    @GetMapping("/business/{businessId}/plan")
    public ResponseEntity<BusinessSubscriptionView> getBusinessPlan(@PathVariable Long businessId) {
        var b = serviceGetBusinessPlan(businessId);
        return b == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(b);
    }
    public BusinessSubscriptionView serviceGetBusinessPlan(Long businessId) {
        return businessSubscriptionRepository.findByBusinessId(businessId)
                .map(com.ipachi.pos.subscriptions.dto.BusinessSubscriptionView::of)
                .orElse(null);
    }

    // Customer: activate code
    @PostMapping("/activate")
    public SubscriptionCodeView activate(@RequestBody ActivateCodeRequest req) {
        return SubscriptionCodeView.of(service.activate(req));
    }

    private static void write(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.UTF_8));
    }
    public static class StartTrialRequest {
        public Long businessId;      // required for now
        public Long activatedByUserId; // optional audit
        public String ip;            // optional audit
    }

    @PostMapping("/trial/start")
    public ResponseEntity<?> startTrial(@RequestBody StartTrialRequest req) {
        if (req.businessId == null) return ResponseEntity.badRequest().body("businessId is required");
        var trial = service.startFreeTrial(req.businessId, req.activatedByUserId, req.ip);
        return ResponseEntity.ok(trial);
    }
    public static class CancelCodeRequest {
        public Long adminUserId; // optional audit
        public String reason;    // optional
    }
    public static class TerminateRequest {
        public Long adminUserId; // optional audit
        public String reason;    // optional
    }

    @PostMapping("/codes/{id}/cancel")
    public SubscriptionCodeView cancel(@PathVariable Long id, @RequestBody(required = false) CancelCodeRequest req) {
        var c = service.cancelCode(id, req != null ? req.adminUserId : null, req != null ? req.reason : null);
        return SubscriptionCodeView.of(c);
    }

    @PostMapping("/codes/{id}/terminate-subscription")
    public ResponseEntity<?> terminate(@PathVariable Long id, @RequestBody(required = false) TerminateRequest req) {
        var b = service.terminateSubscriptionByCode(id, req != null ? req.adminUserId : null, req != null ? req.reason : null);
        return ResponseEntity.ok(com.ipachi.pos.subscriptions.dto.BusinessSubscriptionView.of(b));
    }

    public static class CancelTrialRequest {
        public Long businessId;
    }

    @PostMapping("/trial/cancel")
    public ResponseEntity<?> cancelTrial(@RequestBody CancelTrialRequest req) {
        if (req.businessId == null) return ResponseEntity.badRequest().body("businessId is required");
        service.cancelTrial(req.businessId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/business/{businessId}/effective-plan")
    public EffectivePlanView getEffectivePlan(@PathVariable Long businessId) {
        return service.getEffectivePlan(businessId);
    }

    private static String n(Object o) { return o == null ? "" : o.toString(); }
}
