// src/main/java/com/ipachi/pos/dto/UpdatePromoRequest.java
package com.ipachi.pos.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePromoRequest {
    private BigDecimal newSellPrice; // optional
    private Boolean onSpecial;       // optional
}
