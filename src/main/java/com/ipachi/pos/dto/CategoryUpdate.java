package com.ipachi.pos.dto;

// src/main/java/com/ipachi/pos/inventory/dto/CategoryUpdate.java

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdate(
        @NotBlank @Size(max = 64) String name
) {}

