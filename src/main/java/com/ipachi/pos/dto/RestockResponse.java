package com.ipachi.pos.dto;

// src/main/java/com/ipachi/pos/dto/RestockResponse.java

import java.math.BigDecimal;

public record RestockResponse(
        Long productId,
        BigDecimal quantity // current total after movement
) {}
