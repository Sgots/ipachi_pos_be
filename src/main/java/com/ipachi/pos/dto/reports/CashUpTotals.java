// src/main/java/com/ipachi/pos/dto/reports/CashUpTotals.java
package com.ipachi.pos.dto.reports;

import java.math.BigDecimal;

public record CashUpTotals(
        BigDecimal totalBuyingCash,
        BigDecimal totalProfit,
        BigDecimal cashBalance // = totalBuyingCash + totalProfit
) {}
