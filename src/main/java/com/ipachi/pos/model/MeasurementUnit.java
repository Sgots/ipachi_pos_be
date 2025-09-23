// src/main/java/com/ipachi/pos/model/MeasurementUnit.java
package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inv_measurements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inv_measure_name_per_business", columnNames = {"business_id","name"}),
                @UniqueConstraint(name = "uk_inv_measure_abbr_per_business", columnNames = {"business_id","abbr"})
        })
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementUnit extends BaseOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String abbr;
}
