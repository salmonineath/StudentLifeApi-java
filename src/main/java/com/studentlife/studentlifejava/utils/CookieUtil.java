package com.studentlife.studentlifejava.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    // Single source of truth for these names - they used to be duplicated as
    // string literals in JWTAuthFilter and AuthController, which is one typo away
    // from silently breaking auth.
    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    @Value("${jwt.access-token-expire}")
    private long accessTokenExpireMs;

    @Value("${jwt.refresh-token-expire}")
    private long refreshTokenExpireMs;

    @Value("${app.secure-cookie:true}")
    private boolean secureCookie;

    public ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name,value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                // SameSite=None requires Secure=true - browsers reject the cookie
                // otherwise. Ties directly to secureCookie rather than an env flag so
                // the two can never drift out of sync. Lax is only safe for local
                // http dev where Secure is off anyway.
                .sameSite(secureCookie ? "None" : "Lax")
                .build();
    }

    public void setAccessTokenCookie(HttpServletResponse response, String value) {
        long maxAge = accessTokenExpireMs / 1000;
        response.addHeader("Set-Cookie", buildCookie(ACCESS_TOKEN_COOKIE, value, maxAge).toString());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String value) {
        long maxAge = refreshTokenExpireMs / 1000;
        response.addHeader("Set-Cookie", buildCookie(REFRESH_TOKEN_COOKIE, value, maxAge).toString());
    }

    public void clearAuthCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite(secureCookie ? "None" : "Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
