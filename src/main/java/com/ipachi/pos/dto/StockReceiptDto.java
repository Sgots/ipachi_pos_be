package com.ipachi.pos.dto;

import java.time.OffsetDateTime;

public record StockReceiptDto(
        Long id,
        String label,        // reference or label
        String fileName,
        String contentType,
        Long fileSize,
        String fileUrl,
        OffsetDateTime createdAt,
        OffsetDateTime receiptAt   // <<< new
) {}
