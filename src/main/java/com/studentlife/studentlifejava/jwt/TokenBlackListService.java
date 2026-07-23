package com.studentlife.studentlifejava.jwt;

import com.studentlife.studentlifejava.utils.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlackListService {

    private static final String KEY_PREFIX = "blacklist:access:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JWTService jwtService;

    public void revoke(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return;

        try {
            long remainingMs = jwtService.extractExpiration(accessToken).getTime() - Instant.now().toEpochMilli();
            if (remainingMs <= 0) return;

            stringRedisTemplate.opsForValue().set(key(accessToken), "revoke", Duration.ofMillis(remainingMs));
        } catch (Exception e) {
            log.error("error");
        }
    }

    public boolean isRevoked(String accessToken) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key(accessToken)));
    }

    private String key(String accessToken) {
        return KEY_PREFIX + TokenHashUtil.hash(accessToken);
    }
}
