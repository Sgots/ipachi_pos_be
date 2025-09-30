package com.ipachi.pos.subscriptions;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "subscription_codes", indexes = {
        @Index(name = "ux_subscription_codes_code", columnList = "code", unique = true),
        @Index(name = "ix_subscription_codes_tier", columnList = "tier"),
        @Index(name = "ix_subscription_codes_used_at", columnList = "usedAt")
})
public class SubscriptionCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubscriptionTier tier;

    @Column(nullable = false)
    private OffsetDateTime generatedAt;

    private OffsetDateTime expiresAt;

    private OffsetDateTime usedAt;

    // Link to the entity that redeemed it (e.g., Business/Account). Keep as ID to avoid coupling.
    private Long redeemedByBusinessId;
    // ...existing fields...
    private java.time.OffsetDateTime cancelledAt;
    private Long cancelledByUserId;

    @Column(length = 256)
    private String cancelledReason;

    // getters/setters
    public java.time.OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(java.time.OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public Long getCancelledByUserId() { return cancelledByUserId; }
    public void setCancelledByUserId(Long cancelledByUserId) { this.cancelledByUserId = cancelledByUserId; }
    public String getCancelledReason() { return cancelledReason; }
    public void setCancelledReason(String cancelledReason) { this.cancelledReason = cancelledReason; }

    // --- getters/setters ---
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
    public Long getRedeemedByBusinessId() { return redeemedByBusinessId; }
    public void setRedeemedByBusinessId(Long redeemedByBusinessId) { this.redeemedByBusinessId = redeemedByBusinessId; }
}
