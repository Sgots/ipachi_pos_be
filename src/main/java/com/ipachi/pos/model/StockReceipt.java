// src/main/java/com/ipachi/pos/model/StockReceipt.java
package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "inv_stock_receipt", indexes = {
        @Index(name="idx_inv_receipt_label", columnList = "label")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class StockReceipt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String label;

    @Column(nullable=false)
    private String fileName;

    private String contentType;

    private Long fileSize;

    @Lob
    @Column(name="file_data", columnDefinition = "LONGBLOB")
    private byte[] data;

    @CreationTimestamp
    private Instant createdAt;
}
