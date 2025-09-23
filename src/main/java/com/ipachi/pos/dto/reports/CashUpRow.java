// src/main/java/com/ipachi/pos/dto/reports/CashUpRow.java
package com.ipachi.pos.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class CashUpRow {
    private String product;
    private BigDecimal cash;   // total buying price spent (qty * buyPrice)
    private BigDecimal profit; // Σ((unitPrice - buyPrice) * qty)
}
