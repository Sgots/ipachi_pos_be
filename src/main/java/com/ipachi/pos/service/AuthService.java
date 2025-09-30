package com.ipachi.pos.service;

import com.ipachi.pos.dto.AuthResponse;
import com.ipachi.pos.dto.LoginRequest;
import com.ipachi.pos.dto.RegisterRequest;
import com.ipachi.pos.model.User;
import com.ipachi.pos.model.UserRole;
import com.ipachi.pos.repo.UserRepository;
import com.ipachi.pos.security.JwtService;
import com.ipachi.pos.events.UserCreatedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final ApplicationEventPublisher events;
    private final OtpService otpService;
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        // 0) Basic shape checks
        if (req == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Missing request body");
        }
        if (req.username() == null || req.username().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (req.password() == null || req.password().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (req.areaCode() == null || req.areaCode().isBlank() || req.phone() == null || req.phone().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Phone (areaCode + phone) is required");
        }
        if (req.otp() == null || req.otp().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "OTP is required");
        }

        // 1) Normalize MSISDN and verify OTP BEFORE any insert/update occurs
        final String msisdn = normalizeMsisdn(req.areaCode(), req.phone()); // e.g. "+26774665135"
        boolean otpOk = otpService.verifyAndConsume(msisdn, req.otp());     // must atomically ensure not re-usable
        if (!otpOk) {
            // Nothing persisted yet → safe to bail out
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        // 2) Only now do uniqueness checks (still read-only)
        if (users.existsByUsernameIgnoreCase(req.username())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Username taken");
        }
        if (req.email() != null && !req.email().isBlank() && users.existsByEmail(req.email())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Email taken");
        }

        // 3) Create the user (first write happens AFTER OTP verification)
        User user = User.builder()
                .username(req.username().trim())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .role(UserRole.ADMIN) // or STAFF; up to your flow
                .enabled(true)
                .build();

        users.saveAndFlush(user);

        // 4) Domain event (e.g., create default terminal, etc.)
        events.publishEvent(new UserCreatedEvent(user.getId()));

        // 5) JWT for immediate FE use (optional – you can also force re-login if preferred)
        String token = jwt.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(),
                user.getBusinessProfileId(), user.getTerminalId());
    }

    /** Minimal normalizer that ensures a single leading '+' and strips spaces/dashes. */
    private static String normalizeMsisdn(String areaCode, String phone) {
        String ac = areaCode.trim().replaceAll("[^\\d+]", "");
        String pn = phone.trim().replaceAll("[^\\d]", "");
        if (!ac.startsWith("+")) ac = "+" + ac;
        return ac + pn;
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User user = users.findByUsername(req.username()).orElseThrow();
        return new AuthResponse(jwt.generateToken(user.getUsername()), user.getUsername(), user.getRole().name(), user.getBusinessProfileId(), user.getTerminalId());
    }
}
