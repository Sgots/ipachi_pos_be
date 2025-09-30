package com.ipachi.pos.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BusinessTrialRepository extends JpaRepository<BusinessTrial, Long> {
    Optional<BusinessTrial> findByBusinessId(Long businessId);
    List<BusinessTrial> findByStatusAndEndsAtBefore(TrialStatus status, OffsetDateTime cutoff);
}
