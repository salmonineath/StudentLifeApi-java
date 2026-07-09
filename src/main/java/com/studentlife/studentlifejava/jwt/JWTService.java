package com.studentlife.studentlifejava.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JWTService {

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${jwt.access-token-expire}")
    private long accessTokenExpired;

    @Getter
    @Value("${jwt.refresh-token-expire}")
    private long refreshTokenExpired;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getSignKey() {
        return signingKey;
    }

    public String generateAccessToken(
            String userId,
            String email,
            String username,
            List<String> role
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", "access");

        return buildToken(claims, userId, accessTokenExpired);
    }

    public String generateRefreshToken(String userId) {
        return UUID.randomUUID().toString();
    }

    private String buildToken(
            Map<String, Object> claims,
            String subject,
            long expiration
    ) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSignKey(), Jwts.SIG.HS512)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    /*
     * =========================
     * CLAIM EXTRACTORS
     * =========================
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Single-parse validation: verifies signature, expiry, and token type in one shot.
     * Returns the subject (userId) on success; throws JwtException on any failure.
     */
    public String extractUserIdFromAccessToken(String token) {
        Claims claims = extractAllClaims(token);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new io.jsonwebtoken.JwtException("Token is not an access token");
        }
        return claims.getSubject();
    }

}
