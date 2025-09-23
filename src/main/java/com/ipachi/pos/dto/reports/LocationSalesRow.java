// src/main/java/com/ipachi/pos/dto/reports/LocationSalesRow.java
package com.ipachi.pos.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class LocationSalesRow {
    private String location;
    private BigDecimal total;
}
