package com.ipachi.pos.dto;

// src/main/java/com/ipachi/pos/inventory/dto/CategoryDto.java

import java.time.OffsetDateTime;

public record CategoryDto(Long id, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
