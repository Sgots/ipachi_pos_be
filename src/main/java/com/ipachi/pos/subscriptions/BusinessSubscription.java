package com.ipachi.pos.subscriptions;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "business_subscriptions", indexes = {
        @Index(name = "ix_bsub_business", columnList = "businessId", unique = true)
})
public class BusinessSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner business (your existing BusinessProfile.id or Business.id) */
    @Column(nullable = false)
    private Long businessId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubscriptionTier tier;

    /** Snapshot of plan limits at time of activation (keeps history sane if catalog changes later) */
    @Column(nullable = false)
    private int usersAllowed;

    @Column(nullable = true)
    private Integer qrCodeLimit; // null => unlimited

    @Column(nullable = false)
    private OffsetDateTime activatedAt;

    @Column(nullable = true)
    private OffsetDateTime expiresAt; // optional (from code expiry or billing)

    // --- getters/setters
    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }
    public int getUsersAllowed() { return usersAllowed; }
    public void setUsersAllowed(int usersAllowed) { this.usersAllowed = usersAllowed; }
    public Integer getQrCodeLimit() { return qrCodeLimit; }
    public void setQrCodeLimit(Integer qrCodeLimit) { this.qrCodeLimit = qrCodeLimit; }
    public OffsetDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(OffsetDateTime activatedAt) { this.activatedAt = activatedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
