// src/main/java/com/ipachi/pos/dto/RestockRequest.java
package com.ipachi.pos.dto;

public record RestockRequest(
        java.math.BigDecimal quantity,
        Long receiptId,
        String note
) {}
