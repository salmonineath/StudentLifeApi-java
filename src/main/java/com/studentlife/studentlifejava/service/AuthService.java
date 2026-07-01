package com.studentlife.studentlifejava.service;

import com.studentlife.studentlifejava.dto.AuthResult;
import com.studentlife.studentlifejava.dto.request.AuthRequest;
import com.studentlife.studentlifejava.dto.request.RegisterRequest;

public interface AuthService {

    AuthResult register(RegisterRequest request);

    AuthResult login(AuthRequest request);

    AuthResult refreshToken(String rawRefreshToken);

    void logout(String rawRefreshToken);
}
