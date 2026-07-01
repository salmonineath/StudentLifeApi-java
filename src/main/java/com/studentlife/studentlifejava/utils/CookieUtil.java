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

    @Value("${jwt.access-token-expire}")
    private long accessTokenExpireMs;

    @Value("${jwt.refresh-token-expire}")
    private long refreshTokenExpireMs;

    @Value("${app.secure-cookie:false}")
    private boolean secureCookie;

    public ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name,value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite(secureCookie ? "None" : "Lax")
                .build();
    }

    public void setAccessTokenCookie(HttpServletResponse response, String value) {
        long maxAge = accessTokenExpireMs / 1000;
        response.addHeader("Set-Cookie", buildCookie("accessToken", value, maxAge).toString());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String value) {
        long maxAge = refreshTokenExpireMs / 1000;
        response.addHeader("Set-Cookie", buildCookie("refreshToken", value, maxAge).toString());
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
