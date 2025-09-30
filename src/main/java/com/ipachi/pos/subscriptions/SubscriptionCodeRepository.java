package com.ipachi.pos.subscriptions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SubscriptionCodeRepository extends JpaRepository<SubscriptionCode, Long> {

    Optional<SubscriptionCode> findByCode(String code);

    @Query("""
        select c from SubscriptionCode c
        where (:tier is null or c.tier = :tier)
          and (:used is null or (:used = true and c.usedAt is not null) or (:used = false and c.usedAt is null))
          and (:from is null or c.generatedAt >= :from)
          and (:to is null or c.generatedAt < :to)
        order by c.generatedAt desc, c.id desc
    """)
    Page<SubscriptionCode> search(
            @Param("tier") SubscriptionTier tier,
            @Param("used") Boolean used,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );
}
