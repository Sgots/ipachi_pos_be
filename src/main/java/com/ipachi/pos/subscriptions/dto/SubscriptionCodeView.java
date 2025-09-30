package com.ipachi.pos.subscriptions.dto;

import com.ipachi.pos.subscriptions.SubscriptionTier;
import java.time.OffsetDateTime;

public class SubscriptionCodeView {
    public Long id;
    public String code;
    public SubscriptionTier tier;
    public OffsetDateTime generatedAt;
    public OffsetDateTime expiresAt;
    public OffsetDateTime usedAt;
    public Long redeemedByBusinessId;
    // add fields
    public java.time.OffsetDateTime cancelledAt;
    public String cancelledReason;

// in of(...)


    public static SubscriptionCodeView of(com.ipachi.pos.subscriptions.SubscriptionCode c) {
        var v = new SubscriptionCodeView();
        v.id = c.getId();
        v.code = c.getCode();
        v.tier = c.getTier();
        v.generatedAt = c.getGeneratedAt();
        v.expiresAt = c.getExpiresAt();
        v.usedAt = c.getUsedAt();
        v.redeemedByBusinessId = c.getRedeemedByBusinessId();
        v.cancelledAt = c.getCancelledAt();
        v.cancelledReason = c.getCancelledReason();
        return v;
    }
}
