package com.ipachi.pos.dto;

// src/main/java/com/ipachi/pos/inventory/dto/MeasurementUpdate.java

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MeasurementUpdate(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 16) String abbr
) {}
