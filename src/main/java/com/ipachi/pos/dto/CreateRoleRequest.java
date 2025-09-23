// src/main/java/com/ipachi/pos/dto/role/CreateRoleRequest.java
package com.ipachi.pos.dto;

import java.util.List;

public record CreateRoleRequest(
        String name,
        List<ModulePermissionDto> permissions
) {}
