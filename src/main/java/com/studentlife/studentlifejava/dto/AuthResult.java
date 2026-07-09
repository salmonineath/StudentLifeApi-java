package com.studentlife.studentlifejava.dto;

import com.studentlife.studentlifejava.dto.response.UserResponse;

public record AuthResult(String accessToken, String refreshToken, UserResponse user) {

    public static AuthResult of(String accessToken, String refreshToken) {
        return new AuthResult(accessToken, refreshToken, null);
    }

    public static AuthResult of(String accessToken, String refreshToken, UserResponse user) {
        return new AuthResult(accessToken, refreshToken, user);
    }
}
