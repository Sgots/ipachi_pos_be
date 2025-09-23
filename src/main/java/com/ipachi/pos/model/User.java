package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    /** Staff Number (unique identifier for staff logins) */
    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Nullable until a BusinessProfile is created for this user. */
    @Column(name = "business_profile_id")
    private Long businessProfileId;

    /** Optional terminal association for this user (audit/routing) */
    @Column(name = "terminal_id")
    private String terminalId;
}
