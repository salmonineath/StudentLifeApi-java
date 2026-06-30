package com.studentlife.studentlifejava.Controller;

import com.studentlife.studentlifejava.DTO.AuthResult;
import com.studentlife.studentlifejava.DTO.Request.AuthRequest;
import com.studentlife.studentlifejava.DTO.Request.RegisterRequest;
import com.studentlife.studentlifejava.DTO.Response.ApiResponse;
import com.studentlife.studentlifejava.DTO.Response.AuthResponse;
import com.studentlife.studentlifejava.DTO.Response.RegisterResponse;
import com.studentlife.studentlifejava.Service.AuthService;
import com.studentlife.studentlifejava.Utils.CookieUtil;
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
                201, true, "Registered successfully.",
                new RegisterResponse(result.accessToken(), result.user())
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.login(request);
        cookieUtil.setAccessTokenCookie(response, result.accessToken());
        cookieUtil.setRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(new ApiResponse<>(
                200, true, "Login successfully.",
                new AuthResponse(result.accessToken())
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rawRefreshToken = cookieUtil.getCookieValue(request, "refreshToken");
        AuthResult result = authService.refreshToken(rawRefreshToken);
        cookieUtil.setAccessTokenCookie(response, result.accessToken());
        cookieUtil.setRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(new ApiResponse<>(
                200, true, "Token refreshed successfully.",
                new AuthResponse(result.accessToken())
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rawRefreshToken = cookieUtil.getCookieValue(request, "refreshToken");
        authService.logout(rawRefreshToken);
        cookieUtil.clearAuthCookie(response, "accessToken");
        cookieUtil.clearAuthCookie(response, "refreshToken");
        return ResponseEntity.ok(new ApiResponse<>(200, true, "Logout successfully."));
    }
}
