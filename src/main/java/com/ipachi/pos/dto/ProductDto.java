package com.ipachi.pos.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Product DTO exposed to clients. Includes availableQuantity computed from stock movements.
 */
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
        BigDecimal recipeCost,      // sum of item costs for recipe

        String lifetime,
        Integer lowStock,
        ProductSaleMode saleMode,

        Integer availableQuantity   // computed from stock movements (nullable)
) {}
