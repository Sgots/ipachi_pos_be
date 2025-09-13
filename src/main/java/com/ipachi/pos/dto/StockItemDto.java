package com.ipachi.pos.dto;

// src/main/java/com/ipachi/pos/dto/StockItemDto.java

import java.math.BigDecimal;

public record StockItemDto(
        Long id,
        String sku,
        String barcode,
        String name,

        Long unitId,
        String unitName,
        String unitAbbr,

        BigDecimal quantity,         // computed sum of movements
        Integer lowStock             // optional threshold; null if you don’t track it
) {}
