package com.studentlife.studentlifejava.DTO;

import com.studentlife.studentlifejava.DTO.Response.UserResponse;

public record AuthResult(String accessToken, String refreshToken, UserResponse user) {

    public static AuthResult of(String accessToken, String refreshToken) {
        return new AuthResult(accessToken, refreshToken, null);
    }

    public static AuthResult of(String accessToken, String refreshToken, UserResponse user) {
        return new AuthResult(accessToken, refreshToken, user);
    }
}
