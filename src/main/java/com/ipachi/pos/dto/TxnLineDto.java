package com.ipachi.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Flat line-level DTO:
 * - remainingStock REMOVED
 * - profit ADDED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TxnLineDto {
    private Long txId;                    // Transaction ID (header)
    private OffsetDateTime date;          // Transaction createdAt (header)
    private String sku;                   // Line SKU
    private String name;                  // Line name
    private Integer qty;                  // Line quantity
    private BigDecimal grossAmount;       // Line gross (net + VAT)
    private BigDecimal vatAmount;         // VAT portion at line level
    private BigDecimal profit;            // Per-line profit
    private Long createdByUserId;         // Who created (from line)
    private String terminalId;            // From User.terminal_id (joined)
}
