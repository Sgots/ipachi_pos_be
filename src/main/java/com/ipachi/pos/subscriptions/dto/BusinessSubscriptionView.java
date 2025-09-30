package com.ipachi.pos.subscriptions.dto;

import com.ipachi.pos.subscriptions.BusinessSubscription;
import com.ipachi.pos.subscriptions.SubscriptionTier;

import java.time.OffsetDateTime;

public class BusinessSubscriptionView {
    public Long businessId;
    public SubscriptionTier tier;
    public int usersAllowed;
    public Integer qrCodeLimit; // null => unlimited
    public OffsetDateTime activatedAt;
    public OffsetDateTime expiresAt;

    public static BusinessSubscriptionView of(BusinessSubscription b) {
        var v = new BusinessSubscriptionView();
        v.businessId = b.getBusinessId();
        v.tier = b.getTier();
        v.usersAllowed = b.getUsersAllowed();
        v.qrCodeLimit = b.getQrCodeLimit();
        v.activatedAt = b.getActivatedAt();
        v.expiresAt = b.getExpiresAt();
        return v;
    }
}
