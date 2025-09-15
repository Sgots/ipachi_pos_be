package com.ipachi.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreate(
        String sku,
        String barcode,
        String name,
        BigDecimal buyPrice,           // required for SINGLE and RECIPE (FE is source of truth)
        BigDecimal sellPrice,
        Long categoryId,
        Long unitId,
        ProductType productType,       // SINGLE/RECIPE
        ProductSaleMode saleMode,      // PER_UNIT/BY_WEIGHT
        String lifetime,               // nullable
        Integer lowStock,              // nullable
        List<ProductComponentCreate> components // for RECIPE
) {}
