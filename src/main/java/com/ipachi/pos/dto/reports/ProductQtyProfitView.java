// src/main/java/com/ipachi/pos/dto/reports/ProductQtyProfitView.java
package com.ipachi.pos.dto.reports;

import java.math.BigDecimal;

public interface ProductQtyProfitView {
    String getSku();
    String getName();
    BigDecimal getQty();     // SUM(...) as BigDecimal
    BigDecimal getProfit();  // SUM(...) as BigDecimal
}
