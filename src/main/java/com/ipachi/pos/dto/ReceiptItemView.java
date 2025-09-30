// src/main/java/com/ipachi/pos/dto/ReceiptItemView.java
package com.ipachi.pos.dto;

import java.math.BigDecimal;

public record ReceiptItemView(
        Long productId,
        String sku,
        String name,
        BigDecimal quantity,        // quantityDelta > 0 for this receipt
        BigDecimal unitPrice,       // Product.sellPrice (without VAT)
        BigDecimal value            // quantity * unitPrice
) {}
