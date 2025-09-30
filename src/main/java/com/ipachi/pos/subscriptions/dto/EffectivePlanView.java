package com.ipachi.pos.subscriptions.dto;

import com.ipachi.pos.subscriptions.SubscriptionTier;

import java.time.OffsetDateTime;

public class EffectivePlanView {
    public Long businessId;
    /** TRIAL | SUBSCRIPTION | NONE */
    public String source;
    public SubscriptionTier tier;
    public int usersAllowed;
    public Integer qrCodeLimit; // null => unlimited
    public OffsetDateTime trialEndsAt; // when source = TRIAL
    public OffsetDateTime subscriptionExpiresAt; // when source = SUBSCRIPTION
}
