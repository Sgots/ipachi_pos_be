package com.ipachi.pos.security;


import java.time.Instant;
import java.util.Date;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.*;

@Service
public class JwtService {
    @Value("${security.jwt.secret}") private String secret;
    @Value("${security.jwt.expiry-minutes}") private long expiryMinutes;

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiryMinutes * 60);
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload().getSubject();
    }
}
