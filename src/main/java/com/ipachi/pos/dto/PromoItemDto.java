// src/main/java/com/ipachi/pos/dto/PromoItemDto.java
package com.ipachi.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data @AllArgsConstructor
public class PromoItemDto {
    private Long id;
    private String sku;
    private String barcode;
    private String name;
    private BigDecimal quantity;  // remaining stock (can be 0)
    private String measurement;   // unit
    private Integer lifetimeDays;
    private Integer inStockForDays;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Boolean onSpecial;
}
