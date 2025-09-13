package com.ipachi.pos.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
@NotBlank String username,
@Email @NotBlank String email,
@Size(min=8, message="Password must be at least 8 chars") String password
        ){}
