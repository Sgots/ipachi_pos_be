// src/main/java/com/ipachi/pos/dto/reports/TradeAccountStatement.java
package com.ipachi.pos.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class TradeAccountStatement {
    private BigDecimal sales;         // Σ tx.total
    private BigDecimal openingStock;  // value at start-1s
    private BigDecimal newStock;      // purchases value in period
    private BigDecimal closingStock;  // value at end
    private BigDecimal costOfSales;   // opening + new - closing
    private BigDecimal grossPL;       // sales - costOfSales
}
