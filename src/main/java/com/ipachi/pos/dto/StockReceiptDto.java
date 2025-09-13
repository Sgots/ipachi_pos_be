// src/main/java/com/ipachi/pos/dto/StockReceiptDto.java
package com.ipachi.pos.dto;

import java.time.Instant;

public record StockReceiptDto(
        Long id,
        String label,
        String fileName,
        String contentType,
        Long fileSize,
        String fileUrl,
        Instant createdAt
) {}
