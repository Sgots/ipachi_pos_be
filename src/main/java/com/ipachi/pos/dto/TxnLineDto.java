package com.ipachi.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class TxnLineDto {
    private Long id;                  // Transaction ID (header)
    private OffsetDateTime date;      // Transaction createdAt (header)
    private String name;              // Line name
    private String sku;               // Line SKU
    private Integer qty;              // Line quantity
    private BigDecimal totalAmount;   // Line total
    private BigDecimal profit;        // NEW
    private BigDecimal remainingStock; // NEW
}
