// src/main/java/com/ipachi/pos/model/Product.java
package com.ipachi.pos.model;

import com.ipachi.pos.dto.ProductSaleMode;
import com.ipachi.pos.dto.ProductType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "inv_products",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_inv_product_sku", columnNames = "sku"),
                @UniqueConstraint(name="uk_inv_product_barcode", columnNames = "barcode")
        })
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 64)
    private String barcode;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal buyPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductType productType = ProductType.SINGLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_mode", nullable = false, length = 16)
    private ProductSaleMode saleMode = ProductSaleMode.PER_UNIT;

    @Column(name = "lifetime", length = 64)
    private String lifetime; // e.g., "7 days"

    @Column(name = "low_stock")
    private Integer lowStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private MeasurementUnit unit;

    // IMPORTANT:
    // - Keep a single List instance (never replace it).
    // - Use builder default so Lombok @Builder/@SuperBuilder doesn't set it to null.
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductComponent> components = new ArrayList<>();

    // Image
    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data", columnDefinition = "MEDIUMBLOB")
    private byte[] imageData;

    @Column(name="image_type", length = 100)
    private String imageContentType;

    @Column(name="image_name", length = 255)
    private String imageFilename;

    /* =======================
       Relationship helpers
       ======================= */

    /** Attach a component and keep both sides in sync. */
    public void addComponent(ProductComponent pc) {
        if (pc == null) return;
        components.add(pc);
        pc.setParent(this);
    }

    /** Detach a component and keep both sides in sync. */
    public void removeComponent(ProductComponent pc) {
        if (pc == null) return;
        components.remove(pc);
        pc.setParent(null);
    }

    /** Remove all components owned by a given userId (safe with orphanRemoval). */
    public void removeComponentsForUser(Long userId) {
        for (Iterator<ProductComponent> it = components.iterator(); it.hasNext();) {
            ProductComponent pc = it.next();
            if (Objects.equals(pc.getUserId(), userId)) {
                it.remove();     // mutate the SAME list instance
                pc.setParent(null);
            }
        }
    }

    /**
     * Replace components for a given userId in a Hibernate-safe way:
     * - mutate current list (don’t reassign)
     * - orphanRemoval will delete removed children
     */
    public void replaceComponentsForUser(Long userId, List<ProductComponent> newOnes) {
        // 1) remove old
        removeComponentsForUser(userId);
        // 2) add new
        if (newOnes != null) {
            for (ProductComponent pc : newOnes) {
                if (pc != null) {
                    pc.setUserId(userId);
                    addComponent(pc);
                }
            }
        }
    }
}
