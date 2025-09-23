// src/main/java/com/ipachi/pos/dto/ProductDto.java
package com.ipachi.pos.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductDto(
        Long id,
        String sku,
        String barcode,
        String name,
        BigDecimal buyPrice,
        BigDecimal sellPrice,

        Long categoryId,
        String categoryName,

        Long unitId,
        String unitName,
        String unitAbbr,

        Boolean hasImage,
        String imageUrl,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,

        ProductType productType,
        BigDecimal recipeCost,

        Integer lifetime,
        Integer lowStock,
        ProductSaleMode saleMode,

        Integer availableQuantity,

        // NEW (server-calculated)
        BigDecimal priceInclVat,
        BigDecimal priceExclVat,
        BigDecimal vatRateApplied   // e.g. 14.00
) {}
