package com.ipachi.pos.service;

import com.ipachi.pos.dto.AuthResponse;
import com.ipachi.pos.dto.LoginRequest;
import com.ipachi.pos.dto.RegisterRequest;
import com.ipachi.pos.model.User;
import com.ipachi.pos.model.UserRole;
import com.ipachi.pos.repo.UserRepository;
import com.ipachi.pos.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByUsername(req.username())) throw new IllegalArgumentException("Username taken");
        if (users.existsByEmail(req.email())) throw new IllegalArgumentException("Email taken");
        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .role(UserRole.ADMIN) // or STAFF; up to your flow
                .enabled(true)
                .build();
        users.save(user);
        String token = jwt.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User user = users.findByUsername(req.username()).orElseThrow();
        return new AuthResponse(jwt.generateToken(user.getUsername()), user.getUsername(), user.getRole().name());
    }
}
