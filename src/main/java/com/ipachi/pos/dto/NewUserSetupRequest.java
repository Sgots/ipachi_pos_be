package com.ipachi.pos.dto;


import com.ipachi.pos.model.IdType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record NewUserSetupRequest(
        String title,
        String gender,
        LocalDate dob,
        IdType idType,
        String idNumber,
        String postalAddress,
        String physicalAddress,
        String city,
        String country,
        String areaCode,
        String phone,
        String bizName,
        String bizLocation
) {}
