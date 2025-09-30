package com.ipachi.pos.dto;

import jakarta.validation.constraints.*;

// src/main/java/com/ipachi/pos/dto/RegisterRequest.java

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(

        @NotBlank(message = "Username is required")
        String username,

        // Optional email (validate format only if provided)
        @Email(message = "Invalid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 chars")
        String password,

        @NotBlank(message = "Area code is required")
        // Accepts "+267" or "267" etc.
        @Pattern(regexp = "^\\+?\\d{1,4}$",
                message = "Area code must be digits, optionally starting with +")
        String areaCode,

        @NotBlank(message = "Phone is required")
        // Allow 5–15 digits (E.164 local part)
        @Pattern(regexp = "^\\d{5,15}$",
                message = "Phone must be 5–15 digits")
        String phone,

        @NotBlank(message = "OTP is required")
        // Typical 4–6 digit OTP codes
        @Pattern(regexp = "^\\d{4,6}$",
                message = "OTP must be 4–6 digits")
        String otp
) {}

