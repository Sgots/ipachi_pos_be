// src/main/java/com/ipachi/pos/dto/reports/ProductSalesRow.java
package com.ipachi.pos.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class ProductSalesRow {
    private String sku;
    private String name;
    private BigDecimal total; // sales amount
}
