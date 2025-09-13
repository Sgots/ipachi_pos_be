// src/main/java/com/ipachi/pos/dto/ProductComponentCreate.java
package com.ipachi.pos.dto;

import java.math.BigDecimal;

public record ProductComponentCreate(
        String name,
        String measurement,     // free text
        BigDecimal unitCost     // item cost (line cost)
) {}
