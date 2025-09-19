package com.ipachi.pos.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StockMovementDto(
        Long id,
        Long productId,
        String productName,
        String productSku,
        BigDecimal quantityDelta,
        Long receiptId,
        String receiptReference,
        String note,
        OffsetDateTime createdAt
) {}