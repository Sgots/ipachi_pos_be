// src/main/java/com/ipachi/pos/dto/PromoRawRow.java
package com.ipachi.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Raw row from repository; service will compute inStockForDays. */
@Data @AllArgsConstructor
public class PromoRawRow {
    private Long productId;
    private String sku;
    private String barcode;
    private String name;
    private BigDecimal currentStock;         // sum of movements
    private String unitName;                 // measurement
    private Integer lifetimeDays;            // product.lifetimeDays
    private OffsetDateTime lastRestockedAt;  // max createdAt where delta > 0
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Boolean onSpecial;
}
