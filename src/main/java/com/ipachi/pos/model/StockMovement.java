package com.ipachi.pos.model;

// src/main/java/com/ipachi/pos/model/StockMovement.java

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inv_stock_movements")
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement extends BaseOwnedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NEW: business scope for movements (aligns with Product.businessId)
    @Column(name = "business_id", nullable = false)
    private Long businessId;

    // NEW: who performed the movement
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    // NEW: optional for terminal audit
    @Column(name = "terminal_id")
    private Long terminalId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity_delta", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityDelta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private StockReceipt receipt; // nullable

    private String note;

    // helpful for “recent” queries
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
