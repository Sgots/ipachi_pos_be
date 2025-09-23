// src/main/java/com/ipachi/pos/dto/reports/DashboardKpis.java
package com.ipachi.pos.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class DashboardKpis {
    private long customersServed;
    private BigDecimal totalSales;    // sum(t.total)
    private BigDecimal overallProfit; // sum(lineProfit)
    private String topProduct;        // by total sales amount
}
