// src/main/java/com/ipachi/pos/dto/reports/MonthlySalesRow.java
package com.ipachi.pos.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class MonthlySalesRow {
    private String period;     // YYYY-MM
    private BigDecimal total;  // sales
}
