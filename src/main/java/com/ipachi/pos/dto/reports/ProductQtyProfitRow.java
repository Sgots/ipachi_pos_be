// src/main/java/com/ipachi/pos/dto/reports/ProductQtyProfitRow.java
package com.ipachi.pos.dto.reports;

import java.math.BigDecimal;

public class ProductQtyProfitRow {
    private final String sku;
    private final String name;
    private final Number qty;        // could be Long/Double/BigDecimal depending on SUM()
    private final BigDecimal profit; // expect BigDecimal

    public ProductQtyProfitRow(String sku, String name, Number qty, BigDecimal profit) {
        this.sku = sku;
        this.name = name;
        this.qty = qty;
        this.profit = profit;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public Number getQty() { return qty; }
    public BigDecimal getProfit() { return profit; }
}
