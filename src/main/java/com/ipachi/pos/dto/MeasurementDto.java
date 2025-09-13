package com.ipachi.pos.dto;

// src/main/java/com/ipachi/pos/inventory/dto/MeasurementDto.java

import java.time.OffsetDateTime;

public record MeasurementDto(Long id, String name, String abbr,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

