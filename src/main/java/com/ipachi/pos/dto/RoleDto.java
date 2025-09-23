package com.ipachi.pos.dto;

// src/main/java/com/ipachi/pos/dto/role/RoleDto.java

import java.time.OffsetDateTime;
import java.util.List;

public record RoleDto(
        Long id,
        String name,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ModulePermissionDto> permissions
) {}
