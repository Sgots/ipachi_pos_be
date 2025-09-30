package com.ipachi.pos.subscriptions.dto;

import com.ipachi.pos.subscriptions.SubscriptionTier;
import java.time.OffsetDateTime;

public class GenerateCodesRequest {
    public SubscriptionTier tier;
    public int count = 1;
    public OffsetDateTime expiresAt; // optional
    public String prefix; // optional (e.g., "IPG-")
}
