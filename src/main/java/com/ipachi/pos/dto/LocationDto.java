package com.ipachi.pos.dto;

import java.time.OffsetDateTime;

public record LocationDto(
        Long id,
        String name,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
