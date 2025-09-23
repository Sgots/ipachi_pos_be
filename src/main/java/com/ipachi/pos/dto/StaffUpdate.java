package com.ipachi.pos.dto;

public record StaffUpdate(
        String firstname,
        String lastname,
        String email,       // staff number (username)
        Long roleId,
        Long locationId,
        Boolean active,
        String terminalId
) {}
