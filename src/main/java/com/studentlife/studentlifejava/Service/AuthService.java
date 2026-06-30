package com.studentlife.studentlifejava.Service;

import com.studentlife.studentlifejava.DTO.AuthResult;
import com.studentlife.studentlifejava.DTO.Request.AuthRequest;
import com.studentlife.studentlifejava.DTO.Request.RegisterRequest;

public interface AuthService {

    AuthResult register(RegisterRequest request);

    AuthResult login(AuthRequest request);

    AuthResult refreshToken(String rawRefreshToken);

    void logout(String rawRefreshToken);
}
