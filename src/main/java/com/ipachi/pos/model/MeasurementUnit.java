package com.ipachi.pos.model;

// src/main/java/com/ipachi/pos/inventory/entity/MeasurementUnit.java

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inv_measurements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inv_measure_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_inv_measure_abbr", columnNames = "abbr")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeasurementUnit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String abbr;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
