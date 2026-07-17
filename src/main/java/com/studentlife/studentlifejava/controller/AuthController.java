package com.studentlife.studentlifejava.controller;

import com.studentlife.studentlifejava.dto.AuthResult;
import com.studentlife.studentlifejava.dto.request.AuthRequest;
import com.studentlife.studentlifejava.dto.request.RegisterRequest;
import com.studentlife.studentlifejava.dto.request.ResetPasswordRequest;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.dto.response.RegisterResponse;
import com.studentlife.studentlifejava.service.AuthService;
import com.studentlife.studentlifejava.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.register(request);
        cookieUtil.setAccessTokenCookie(response, result.accessToken());
        cookieUtil.setRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.status(201).body(new ApiResponse<>(

                201,
                true,
                "Registered successfully.",
                new RegisterResponse(result.user())
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.login(request);
        cookieUtil.setAccessTokenCookie(response, result.accessToken());
        cookieUtil.setRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(new ApiResponse<>(

                200,
                true,
                "Login successfully."
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rawRefreshToken = cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE);
        AuthResult result = authService.refreshToken(rawRefreshToken);
        cookieUtil.setAccessTokenCookie(response, result.accessToken());
        cookieUtil.setRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(new ApiResponse<>(
                200,
                true,
                "Token refreshed successfully."
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rawRefreshToken = cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE);
        authService.logout(rawRefreshToken);
        cookieUtil.clearAuthCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE);
        cookieUtil.clearAuthCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE);
        return ResponseEntity.ok(new ApiResponse<>(
                200,
                true,
                "Logout successfully."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
            ) {
        authService.resetPassword(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        true,
                        "Password reset successfully"
                )
        );
    }
}
