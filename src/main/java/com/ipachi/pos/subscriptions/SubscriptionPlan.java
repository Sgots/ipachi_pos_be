package com.ipachi.pos.subscriptions;

import jakarta.persistence.*;

@Entity
@Table(name = "subscription_plans", uniqueConstraints = {
        @UniqueConstraint(name = "ux_subscription_plans_tier", columnNames = "tier")
})
public class SubscriptionPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubscriptionTier tier;

    /** Max number of app users (staff logins) allowed by the plan */
    @Column(nullable = false)
    private int usersAllowed;

    /** Max number of QR codes (nullable = unlimited) */
    @Column(nullable = true)
    private Integer qrCodeLimit; // null => unlimited

    // --- getters/setters
    public Long getId() { return id; }
    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }
    public int getUsersAllowed() { return usersAllowed; }
    public void setUsersAllowed(int usersAllowed) { this.usersAllowed = usersAllowed; }
    public Integer getQrCodeLimit() { return qrCodeLimit; }
    public void setQrCodeLimit(Integer qrCodeLimit) { this.qrCodeLimit = qrCodeLimit; }
}
