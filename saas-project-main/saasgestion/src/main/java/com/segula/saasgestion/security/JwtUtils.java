package com.segula.saasgestion.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private final SecretKey key;
    private final long accessTokenMs;
    private final long refreshTokenMs;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ms:900000}") long accessTokenMs,
            @Value("${app.jwt.refresh-token-ms:604800000}") long refreshTokenMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMs  = accessTokenMs;
        this.refreshTokenMs = refreshTokenMs;
    }

    // ── Access token ──────────────────────────────────────────────
    public String generateAccessToken(Long userId, String email, String role, boolean forceChange) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .claim("forceChange", forceChange)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenMs))
                .signWith(key)
                .compact();
    }

    // ── Refresh token ─────────────────────────────────────────────
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenMs))
                .signWith(key)
                .compact();
    }

    // ── Parse / validate ─────────────────────────────────────────
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public boolean isForceChange(String token) {
        Boolean fc = parseToken(token).get("forceChange", Boolean.class);
        return Boolean.TRUE.equals(fc);
    }

    public long getRefreshTokenMs() { return refreshTokenMs; }
}