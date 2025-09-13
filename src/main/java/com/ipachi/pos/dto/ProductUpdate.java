// src/main/java/com/ipachi/pos/dto/ProductUpdate.java
package com.ipachi.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpdate(
        String sku,
        String barcode,
        String name,
        BigDecimal buyPrice,
        BigDecimal sellPrice,
        Long categoryId,
        Long unitId,
        ProductType productType,
        ProductSaleMode saleMode,
        String lifetime,
        Integer lowStock,
        List<ProductComponentCreate> components
) {}
