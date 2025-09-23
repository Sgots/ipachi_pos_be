// src/main/java/com/ipachi/pos/dto/reports/CashUpTotals.java
package com.ipachi.pos.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class CashUpTotals {
    private BigDecimal totalCash;    // sum cash
    private BigDecimal totalProfit;  // sum profit
    private BigDecimal cashBalance;  // totalCash + totalProfit
}
