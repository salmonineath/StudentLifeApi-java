package com.studentlife.studentlifejava.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@Order(1)
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/otp/request",
            "/api/v1/auth/otp/verify",
            "/api/v1/assignments/*/invites",
            "/api/v1/users/search"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    // Caffeine cache replaces the old unbounded ConcurrentHashMap.
    // Entries expire 2 windows after last access, capped at 100k IPs to bound memory.
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(WINDOW.multipliedBy(2))
            .maximumSize(100_000)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        boolean limited = RATE_LIMITED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
        if (!limited) {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        Bucket bucket = buckets.get(ip, k -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<Void> body = new ApiResponse<>(429, false, "Too many requests. Please slow down.");
            response.getWriter().write(objectMapper.writeValueAsString(body));
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(MAX_REQUESTS)
                        .refillIntervally(MAX_REQUESTS, WINDOW)
                        .build())
                .build();
    }

    // Resolution priority matches the trust chain in this stack:
    // Cloudflare tunnel → nginx (X-Real-IP) → generic proxy → direct connection
    private String resolveClientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.strip();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.strip();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
