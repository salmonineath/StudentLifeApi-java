package com.studentlife.studentlifejava.Service;

import com.studentlife.studentlifejava.DTO.Request.AuthRequest;
import com.studentlife.studentlifejava.DTO.Request.RegisterRequest;
import com.studentlife.studentlifejava.DTO.Response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    ApiResponse<?> refreshToken(HttpServletRequest request, HttpServletResponse response);

    ApiResponse<?> register(RegisterRequest request, HttpServletResponse response);

    ApiResponse<?> login(AuthRequest request, HttpServletResponse response);

    ApiResponse<Object> logout(HttpServletResponse response, HttpServletRequest request);
}
