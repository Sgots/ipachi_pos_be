package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "staff_locations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_location_business_name", columnNames = {"business_id", "name"})
        },
        indexes = {
                @Index(name = "ix_location_business", columnList = "business_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Location {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner scope */
    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
