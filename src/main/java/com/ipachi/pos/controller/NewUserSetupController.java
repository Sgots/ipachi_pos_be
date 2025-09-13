package com.ipachi.pos.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipachi.pos.dto.NewUserSetupRequest;
import com.ipachi.pos.dto.NewUserSetupResponse;
import com.ipachi.pos.service.NewUserSetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class NewUserSetupController {

    private final NewUserSetupService service;
    private final ObjectMapper objectMapper;


    /**
     * Submit/Update new user setup with files.
     * Frontend should send multipart/form-data with:
     *  - Part "data": JSON of NewUserSetupRequest
     *  - Part "picture": image/*
     *  - Part "idDoc": image/* or application/pdf
     *  - Part "bizLogo": image/*
     */
    @PostMapping(value = "/setup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NewUserSetupResponse> submit(
            @AuthenticationPrincipal UserDetails principal,
            @RequestPart("data") String dataJson,             // <— parse manually
            @RequestPart(value = "picture", required = false) MultipartFile picture,
            @RequestPart(value = "idDoc",   required = false) MultipartFile idDoc,
            @RequestPart(value = "bizLogo", required = false) MultipartFile bizLogo
    ) throws Exception {

        final NewUserSetupRequest data = objectMapper.readValue(dataJson, NewUserSetupRequest.class);

        // simple guard so we fail with 400 instead of DB constraint error
        if (!StringUtils.hasText(data.bizName())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Business name is required");
        }

        var res = service.submit(principal.getUsername(), data, picture, idDoc, bizLogo);
        return ResponseEntity.ok(res);
    }

    /** Read current user's setup */
    @GetMapping("/setup")
    public ResponseEntity<NewUserSetupResponse> me(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.read(principal.getUsername()));
    }
}
