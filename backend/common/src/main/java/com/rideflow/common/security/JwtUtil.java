package com.rideflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and validates JWTs (HMAC-SHA256).
 * All services share the same secret, so a token issued by user-service
 * is trusted by every other service without a network call.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(AuthUser user) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(user.id())
                .claim("name", user.name())
                .claim("role", user.role());
        if (user.vehicle() != null) {
            builder.claim("vehicle", user.vehicle());
        }
        return builder
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Throws a JwtException if the token is invalid or expired. */
    public AuthUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthUser(
                claims.getSubject(),
                claims.get("name", String.class),
                claims.get("role", String.class),
                claims.get("vehicle", String.class));
    }
}
