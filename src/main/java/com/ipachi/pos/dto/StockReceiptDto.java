package com.ipachi.pos.dto;

import java.time.OffsetDateTime;

public record StockReceiptDto(
        Long id,
        String label,  // This might be the reference number
        String fileName,
        String contentType,
        Long fileSize,
        String fileUrl,
        OffsetDateTime createdAt
) {}