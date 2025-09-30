package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/** Stores one OTP attempt for a phone number. */
@Entity
@Table(
        name = "otp_entries",
        indexes = {
                @Index(name = "idx_otp_phone_created", columnList = "phone,createdAt"),
                @Index(name = "idx_otp_phone_verified", columnList = "phone,verified")
        }
)
@Data
public class OtpEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** E.164 phone, e.g. +26774665135 */
    @Column(nullable = false, length = 32)
    private String phone;

    /** BCrypt hash of the OTP code */
    @Column(nullable = false, length = 100)
    private String codeHash;

    /** Expiry moment (UTC) */
    @Column(nullable = false)
    private Instant expiresAt;

    /** How many times the user tried to verify this OTP */
    @Column(nullable = false)
    private int attempts;

    /** Whether this OTP was successfully verified (consumed) */
    @Column(nullable = false)
    private boolean verified;

    /** Creation moment (UTC). Defaulted on insert. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
