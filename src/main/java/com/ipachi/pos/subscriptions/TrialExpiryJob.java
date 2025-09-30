package com.ipachi.pos.subscriptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

@Component
public class TrialExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(TrialExpiryJob.class);
    private final SubscriptionCodeService service;

    public TrialExpiryJob(SubscriptionCodeService service) {
        this.service = service;
    }

    /** Run hourly to be safe. Cron: second minute hour day month weekday */
    @Transactional
    @Scheduled(cron = "0 5 * * * *")
    public void expireTrials() {
        int n = service.expireOverdueTrials();
        if (n > 0) log.info("Expired {} overdue trials", n);
    }
}
