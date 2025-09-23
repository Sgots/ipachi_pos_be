package com.ipachi.pos.dto;

/**
 * Frontend sends:
 *  - firstname, lastname
 *  - email (used as Staff Number) => maps to User.username
 *  - roleId, locationId
 *  - active
 *  - terminalId (string, optional)
 */
public record StaffCreate(
        String firstname,
        String lastname,
        String email,
        Long roleId,
        Long locationId,
        Boolean active,
        String terminalId
) {}
