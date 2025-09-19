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

    // NEW: how it’s sold at POS
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_mode", nullable = false, length = 16)
    private ProductSaleMode saleMode = ProductSaleMode.PER_UNIT;

    // NEW: lifetime and low stock
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

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ProductComponent> components = new java.util.ArrayList<>();

    // Image
    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data", columnDefinition = "MEDIUMBLOB")
    private byte[] imageData;

    @Column(name="image_type", length = 100)
    private String imageContentType;

    @Column(name="image_name", length = 255)
    private String imageFilename;


}
