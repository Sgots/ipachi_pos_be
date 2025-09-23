// src/main/java/com/ipachi/pos/dto/reports/CategoryProfitRow.java
package com.ipachi.pos.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class CategoryProfitRow {
    private String category;
    private BigDecimal profit;
}
