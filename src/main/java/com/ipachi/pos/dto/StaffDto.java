package com.ipachi.pos.dto;

public record StaffDto(
        Long id,
        String firstname,
        String lastname,
        // UI maps "email" to staff number column
        String email,
        Long roleId,
        String roleName,
        Long locationId,
        String locationName,
        Boolean active,
        String terminalId
) {}
