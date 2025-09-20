// src/main/java/com/ipachi/pos/model/ProductComponent.java
package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "product_components")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProductComponent extends BaseOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Product parent;

    // Ingredient linked to a product (optional). NULL means "manual ingredient"
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "component_id")
    private Product component;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private MeasurementUnit unit;

    // Manual ingredient display name (when component == null)
    @Column(name = "name")
    private String name;

    // Free-text measurement (e.g. "2 cups", "1 pack")
    @Column(name = "measurement_text", length = 255)
    private String measurementText;

    // Item cost (NOT multiplied by measurementText)
    @Column(precision = 19, scale = 4)
    private BigDecimal unitCost;
}
