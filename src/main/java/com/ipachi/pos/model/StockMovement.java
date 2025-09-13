package com.ipachi.pos.model;

// src/main/java/com/ipachi/pos/model/StockMovement.java

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inv_stock_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity_delta", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityDelta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private StockReceipt receipt; // nullable

    private String note;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
