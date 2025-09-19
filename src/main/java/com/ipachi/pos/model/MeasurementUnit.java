package com.ipachi.pos.model;

// src/main/java/com/ipachi/pos/inventory/entity/MeasurementUnit.java

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inv_measurements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inv_measure_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_inv_measure_abbr", columnNames = "abbr")
        }
)
@Getter @Setter @SuperBuilder
public class MeasurementUnit extends BaseOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String abbr;


}
