// src/main/java/com/ipachi/pos/dto/OutOfStockDto.java
package com.ipachi.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutOfStockDto {
    private String sku;
    private String barcode;
    private String name;
    private BigDecimal quantity;  // remaining stock (0 or less)
    private String measurement;   // unit name/symbol, e.g., "unit", "kg"
    private String status;        // "Out of stock"

    public OutOfStockDto(String sku, String barcode, String name, BigDecimal quantity, String measurement) {
        this.sku = sku;
        this.barcode = barcode;
        this.name = name;
        this.quantity = quantity;
        this.measurement = measurement;
        this.status = "Out of stock";
    }
}
