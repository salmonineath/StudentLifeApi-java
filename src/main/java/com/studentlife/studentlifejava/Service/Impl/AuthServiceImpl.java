package com.studentlife.studentlifejava.Service.Impl;

import com.studentlife.studentlifejava.DTO.Request.AuthRequest;
import com.studentlife.studentlifejava.DTO.Request.RegisterRequest;
import com.studentlife.studentlifejava.DTO.Response.ApiResponse;
import com.studentlife.studentlifejava.DTO.Response.AuthResponse;
import com.studentlife.studentlifejava.DTO.Response.UserResponse;
import com.studentlife.studentlifejava.Entity.RefreshToken;
import com.studentlife.studentlifejava.Entity.Role;
import com.studentlife.studentlifejava.Entity.User;
import com.studentlife.studentlifejava.JWT.JWTService;
import com.studentlife.studentlifejava.Mapper.UserMapper;
import com.studentlife.studentlifejava.Repository.RefreshTokenRepository;
import com.studentlife.studentlifejava.Repository.RoleRepository;
import com.studentlife.studentlifejava.Repository.UserRepository;
import com.studentlife.studentlifejava.Service.AuthService;
import com.studentlife.studentlifejava.Utils.CookieUtil;
import com.studentlife.studentlifejava.Utils.TokenHashUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.studentlife.studentlifejava.Exception.ErrorsExceptionFactory.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final CookieUtil cookieUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashUtil tokenHashUtil;

    @Override
    @Transactional
    public ApiResponse<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {

//        String refreshTokenValue = cookieUtil.getCookieValue(request, "refreshToken");
//
//        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
//            throw unauthorized("refreshToken is missing");
//        }
//
//        String inComingHash = TokenHashUtil.hash(refreshTokenValue);
//
//        RefreshToken storedToken = refreshTokenRepository
//                .findByTokenHash(refreshTokenValue)
//                .orElseThrow(() -> {
//                    logger.warn("Refresh attempt with unknown token hash");
//                    return unauthorized("Invalid refresh token");
//                });
//
//        User user = storedToken.getUser();
//
//        if (storedToken.isRevoke()) {
//            logger.warn(
//                    "SECURITY ALERT: Refresh token reuse detected for user id={}. " + "Revoking all sessions. Possible token theft.",
//                    user.getId()
//            );
//
//            refreshTokenRepository.revokeALlByUser(user);
//
//            cookieUtil.clearAuthCookie(response, "refreshToken");
//            cookieUtil.clearAuthCookie(response, "accessToken");
//
//            throw unauthorized("Session invalidated. Please log in again.");
//        }
//
//        if (storedToken.getExpiredAt().isBefore(Instant.now())) {
//            refreshTokenRepository.delete(storedToken);
//            cookieUtil.clearAuthCookie(response, "refreshToken");
//            throw unauthorized("Refresh token has expired. Please log in again");
//        }
//
//        storedToken.setRevoke(true);
//        refreshTokenRepository.save(storedToken);
//
//        List<String> role = user.getRoles().stream().map(Role::getName).toList();
//        String newAccessToken = jwtService.generateAccessToken(
//                String.valueOf(user.getId()),
//                user.getEmail(),
//                user.getUsername(),
//                role
//        );
//
////        jwtService.generateAccessToken(user, response);
//
//        cookieUtil.setAccessTokenCookie(response, newAccessToken);
        return null;
    }

    @Override
    public ApiResponse<?> register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.findByEmail(request.getEmail()).isPresent() || userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw validation("This email or username already been used.");
        }

        User user = userMapper.toUserEntityRegisterUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role defaultRole = roleRepository.findByName("user")
                .orElseThrow(() -> notFound("Default role not found."));
        user.setRoles(new HashSet<>(Set.of(defaultRole)));

        User savedUser = userRepository.save(user);

        List<String> role = user.getRoles()
                .stream()
                .map(Role::getName)
                .toList();

        String accessToken = jwtService.generateAccessToken(
                String.valueOf(savedUser.getId()),
                savedUser.getEmail(),
                savedUser.getUsername(),
                role
        );

        cookieUtil.setAccessTokenCookie(response, accessToken);

        UserResponse userResponse = userMapper.toUserResponse(savedUser);

        return new ApiResponse<>(
                201,
                true,
                "Registered successfully.",
                new AuthResponse(accessToken, userResponse)
        );
    }

    @Override
    public ApiResponse<?> login(AuthRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail_or_username())
                .orElseThrow(() -> notFound("User not found."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw validation("Invalid credentials.");
        }

        List<String> role = user.getRoles()
                .stream()
                .map(Role::getName)
                .toList();

        String accessToken = jwtService.generateAccessToken(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getUsername(),
                role
        );
        String refreshToken = jwtService.generateRefreshToken(String.valueOf(user.getId()));

        cookieUtil.setAccessTokenCookie(response, accessToken);
        cookieUtil.setRefreshTokenCookie(response, refreshToken);

        UserResponse userResponse = userMapper.toUserResponse(user);
        return new ApiResponse<>(
                200,
                true,
                "Login successfully.",
                new AuthResponse(accessToken, userResponse)
        );
    }

    @Override
    public ApiResponse<Object> logout(HttpServletResponse response, HttpServletRequest request) {

        String refreshTokenValue = cookieUtil.getCookieValue(request, "refreshToken");

        if (refreshTokenValue != null) {
            refreshTokenRepository.findByTokenHash(refreshTokenValue)
                    .ifPresent(refreshTokenRepository::delete);
        }

        cookieUtil.clearAuthCookie(response, "accessToken");
        cookieUtil.clearAuthCookie(response, "refreshToken");

        return new ApiResponse<>(
                200,
                true,
                "Logout successfully."
        );
    }
}
