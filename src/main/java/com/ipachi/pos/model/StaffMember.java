package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "staff_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_staff_business_user", columnNames = {"business_id", "user_id"})
        },
        indexes = {
                @Index(name = "ix_staff_business", columnList = "business_id"),
                @Index(name = "ix_staff_role", columnList = "role_id"),
                @Index(name = "ix_staff_location", columnList = "location_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner scope */
    @Column(name = "business_id", nullable = false)
    private Long businessId;

    /** Backed by users table (username = staff number) */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Role with module permissions */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** Physical location */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
