package com.ipachi.pos.subscriptions;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "business_trials", indexes = {
        @Index(name = "ux_btrial_business_once", columnList = "businessId", unique = true),
        @Index(name = "ix_btrial_ends", columnList = "endsAt")
})
public class BusinessTrial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubscriptionTier tier = SubscriptionTier.PLATINUM; // fixed for this campaign

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TrialStatus status = TrialStatus.ACTIVE;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    @Column(nullable = false)
    private OffsetDateTime endsAt;

    /** Optional auditing */
    private Long activatedByUserId;
    private String activatedFromIp;

    // getters/setters
    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }
    public TrialStatus getStatus() { return status; }
    public void setStatus(TrialStatus status) { this.status = status; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public Long getActivatedByUserId() { return activatedByUserId; }
    public void setActivatedByUserId(Long activatedByUserId) { this.activatedByUserId = activatedByUserId; }
    public String getActivatedFromIp() { return activatedFromIp; }
    public void setActivatedFromIp(String activatedFromIp) { this.activatedFromIp = activatedFromIp; }
}
