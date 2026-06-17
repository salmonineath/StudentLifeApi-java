package com.studentlife.studentlifejava.JWT;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JWTService {

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    @Value("${spring.jwt-secret}")
    private String jwtSecret;

    @Value("${spring.access-token-expire}")
    private long accessTokenExpired;

    @Value("${spring.refresh-token-expire}")
    private long refreshTokenExpired;

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
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

    public String generateAccessToken(
            String userId,
            String email,
            String username,
            List<String> role
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "access");

        return buildToken(claims, userId, refreshTokenExpired);
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
}
