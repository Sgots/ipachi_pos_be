package com.ipachi.pos.controller;

import com.ipachi.pos.dto.OtpRequestDTO;
import com.ipachi.pos.dto.OtpResponseDTO;
import com.ipachi.pos.dto.OtpVerifyDTO;
import com.ipachi.pos.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// src/main/java/com/ipachi/pos/controller/OtpController.java
@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otp;

    @PostMapping("/request")
    public OtpResponseDTO request(@RequestBody OtpRequestDTO req) {
        if (!StringUtils.hasText(req.phone()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone required");
        otp.request(req.phone().trim());
        return new OtpResponseDTO("OK", "OTP sent");
    }

    @PostMapping("/verify")
    public OtpResponseDTO verify(@RequestBody OtpVerifyDTO req) {
        otp.verify(req.phone().trim(), req.code().trim());
        return new OtpResponseDTO("OK", "Verified");
    }
}

