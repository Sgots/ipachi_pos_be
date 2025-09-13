// src/main/java/com/ipachi/pos/model/ProductComponent.java
package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_components")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductComponent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Product parent;

    // Ingredient linked to a product (optional). NULL means “manual ingredient”
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "component_id", nullable = true)
    private Product component;

    // OPTIONAL: keep if you still use units for ingredients; otherwise remove safely
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = true)
    private MeasurementUnit unit;

    // ✅ NEW: manual ingredient display name (when component == null)
    @Column(name = "name")
    private String name;

    // ✅ NEW: free-text measurement (e.g. “2 cups”, “1 pack”)
    @Column(name = "measurement_text", length = 255)
    private String measurementText;

    // item cost (NOT multiplied by measurementText)
    @Column(precision = 19, scale = 4)
    private BigDecimal unitCost;
}
