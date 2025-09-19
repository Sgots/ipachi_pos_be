package com.ipachi.pos.controller;

import com.ipachi.pos.dto.AuthResponse;
import com.ipachi.pos.dto.LoginRequest;
import com.ipachi.pos.dto.RegisterRequest;
import com.ipachi.pos.model.User;
import com.ipachi.pos.repo.UserRepository;
import com.ipachi.pos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final UserRepository users;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest req) {
        return ResponseEntity.ok(service.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(service.login(req));
    }

    /**
     * Return basic information about the authenticated user.
     * This now includes the database id so the frontend can persist activeUserId.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails user) {
        if (user == null || user.getUsername() == null) {
            return ResponseEntity.ok(Map.of());
        }
        return users.findByUsername(user.getUsername())
                .map(u -> ResponseEntity.ok(Map.of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "email", u.getEmail()
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of("username", user.getUsername())));
    }
}
