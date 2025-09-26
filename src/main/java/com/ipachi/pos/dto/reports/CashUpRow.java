// src/main/java/com/ipachi/pos/dto/reports/CashUpRow.java
package com.ipachi.pos.dto.reports;

import java.math.BigDecimal;

public record CashUpRow(
        String name,
        String sku,
        BigDecimal buyingCash,  // buyPrice * Σqty
        BigDecimal profit,      // Σline.profit
        BigDecimal effectiveTotal // buyingCash + profit (per product)
) {}
