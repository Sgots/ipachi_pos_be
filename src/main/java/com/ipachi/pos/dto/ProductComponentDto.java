// src/main/java/com/ipachi/pos/dto/ProductComponentDto.java
package com.ipachi.pos.dto;

import java.math.BigDecimal;

public record ProductComponentDto(
        Long id,
        Long productId,         // nullable; for manual lines stays null
        String productName,
        String sku,
        Long unitId,            // kept for compatibility (null for manual)
        String unitName,
        String unitAbbr,
        String measurement,     // FREE-TEXT
        BigDecimal unitCost,    // item cost
        BigDecimal lineCost,    // = unitCost
        String name             // manual display name (fallback)
) {}
