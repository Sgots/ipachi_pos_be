package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tx_line")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class TransactionLine extends BaseOwnedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ====== Ownership + Audit ====== */
    @Column(name = "business_id", nullable = false)
    private Long businessId;           // same as head

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;      // who added the line (usually same as head)

    /* ====== Link ====== */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tx_id")
    private Transaction transaction;

    /* ====== Data ====== */
    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer qty;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal; // legacy (unit * qty), kept for compat

    // NEW — authoritative breakdown
    @Column(name = "net_amount",   precision = 18, scale = 2, nullable = false)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "vat_amount",   precision = 18, scale = 2, nullable = false)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "gross_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    // Snapshot of VAT % used (e.g., 14.00)
    @Column(name = "vat_rate_applied", precision = 5, scale = 2)
    private BigDecimal vatRateApplied;

    /* NEW: per-line profit = (unitPrice - product.buyPrice) * qty */
    @Column(name = "profit", nullable = false, precision = 18, scale = 2)
    private BigDecimal profit;

    /* NEW: stock left for the SKU after this sale (business-scoped) */
    @Column(name = "remaining_stock", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingStock;

    @CreationTimestamp
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
