package com.ipachi.pos.repo;

import com.ipachi.pos.model.OtpEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntry, Long> {

    /** Latest OTP (any state) for a phone */
    Optional<OtpEntry> findTopByPhoneOrderByCreatedAtDesc(String phone);

    /** Latest UNVERIFIED OTP for a phone (used by verifyAndConsume) */
    Optional<OtpEntry> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);

    /** Has there been any successful verification for this phone? */
    boolean existsByPhoneAndVerifiedTrue(String phone);
}
