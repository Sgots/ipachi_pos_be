package com.ipachi.pos.dto;

/** Returns the created staff row plus the generated password (plaintext shown once). */
public record StaffCreateResponse(
        StaffDto staff,
        String password
) {}
