package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tx_head")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class Transaction extends BaseOwnedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ====== Ownership + Audit ====== */
    @Column(name = "business_id", nullable = false)
    private Long businessId;           // which business owns this tx

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;      // who rang it up

    @Column(name = "terminal_id")
    private Long terminalId;           // optional terminal

    /* ====== Core fields ====== */
    @CreationTimestamp
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private String customerName = "Walk-in";

    @Column(precision = 18, scale = 2) private BigDecimal total = BigDecimal.ZERO;

    // NEW: authoritative tax totals
    @Column(name = "subtotal_net",  precision = 18, scale = 2, nullable = false)
    private BigDecimal subtotalNet = BigDecimal.ZERO;

    @Column(name = "total_vat",     precision = 18, scale = 2, nullable = false)
    private BigDecimal totalVat = BigDecimal.ZERO;

    @Column(name = "total_gross",   precision = 18, scale = 2, nullable = false)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionLine> lines = new ArrayList<>();
    /* ====== Lines ====== */

}
