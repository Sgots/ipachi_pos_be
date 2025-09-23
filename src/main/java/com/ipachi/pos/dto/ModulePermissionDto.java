// src/main/java/com/ipachi/pos/dto/role/ModulePermissionDto.java
package com.ipachi.pos.dto;

import com.ipachi.pos.model.RoleModule;

public record ModulePermissionDto(
        RoleModule module,
        boolean create,
        boolean view,
        boolean edit,
        boolean delete
) {}
