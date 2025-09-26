package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inv_stock_receipt", indexes = {
        @Index(name = "idx_inv_receipt_label", columnList = "label"),
        @Index(name = "idx_inv_receipt_user_id", columnList = "user_id"),
        @Index(name = "idx_inv_receipt_business_id", columnList = "business_id"),
        @Index(name = "idx_inv_receipt_at", columnList = "receipt_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockReceipt extends BaseOwnedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner scope: which business this receipt belongs to */
    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String fileName;

    private String contentType;

    private Long fileSize;

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] data;

    /** User-chosen date for the receipt (used for opening/closing stock calcs) */
    @Column(name = "receipt_at", nullable = false)
    private OffsetDateTime receiptAt;
}
