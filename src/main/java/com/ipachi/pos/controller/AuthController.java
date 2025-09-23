package com.ipachi.pos.controller;

import com.ipachi.pos.dto.AuthResponse;
import com.ipachi.pos.dto.LoginRequest;
import com.ipachi.pos.dto.RegisterRequest;
import com.ipachi.pos.repo.UserRepository;
import com.ipachi.pos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final UserRepository users;

    // BEFORE: @PreAuthorize("hasRole('ADMIN')")
    // AFTER: allow if first-user or valid setup code, else require ADMIN
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest req) {
        return ResponseEntity.ok(service.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(service.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserDetails user) {
        if (user == null || user.getUsername() == null) {
            return ResponseEntity.ok(Collections.emptyMap());
        }

        return users.findByUsername(user.getUsername())
                .map(u -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("id", u.getId());
                    body.put("username", u.getUsername());
                    // this may be null; LinkedHashMap allows null values
                    body.put("businessProfileId", u.getBusinessProfileId());
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("username", user.getUsername());
                    return ResponseEntity.ok(body);
                });
    }
}
