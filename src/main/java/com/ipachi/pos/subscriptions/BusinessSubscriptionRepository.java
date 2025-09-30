package com.ipachi.pos.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessSubscriptionRepository extends JpaRepository<BusinessSubscription, Long> {
    Optional<BusinessSubscription> findByBusinessId(Long businessId);
}
