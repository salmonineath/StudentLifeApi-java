package com.studentlife.studentlifejava.Service.Impl;

import com.studentlife.studentlifejava.DTO.AuthResult;
import com.studentlife.studentlifejava.DTO.Request.AuthRequest;
import com.studentlife.studentlifejava.DTO.Request.RegisterRequest;
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
import com.studentlife.studentlifejava.Utils.TokenHashUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashUtil tokenHashUtil;

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()
                || userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw validation("This email or username already been used.");
        }

        User user = userMapper.toUserEntityRegisterUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role defaultRole = roleRepository.findByName("user")
                .orElseThrow(() -> notFound("Default role not found."));
        user.setRoles(new HashSet<>(Set.of(defaultRole)));

        User savedUser = userRepository.save(user);

        List<String> roles = savedUser.getRoles().stream().map(Role::getName).toList();

        String accessToken = jwtService.generateAccessToken(
                String.valueOf(savedUser.getId()),
                savedUser.getEmail(),
                savedUser.getUsername(),
                roles
        );
        String refreshToken = jwtService.generateRefreshToken(String.valueOf(savedUser.getId()));

        saveRefreshToken(savedUser, refreshToken);

        UserResponse userResponse = userMapper.toUserResponse(savedUser);
        return AuthResult.of(accessToken, refreshToken, userResponse);
    }

    @Override
    @Transactional
    public AuthResult login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail_or_username())
                .orElseThrow(() -> notFound("User not found."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw validation("Invalid credentials.");
        }

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();

        String accessToken = jwtService.generateAccessToken(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getUsername(),
                roles
        );
        String refreshToken = jwtService.generateRefreshToken(String.valueOf(user.getId()));

        saveRefreshToken(user, refreshToken);

        return AuthResult.of(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResult refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw unauthorized("Refresh token is missing.");
        }

        String incomingHash = TokenHashUtil.hash(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHash(incomingHash)
                .orElseThrow(() -> {
                    log.warn("Refresh attempt with unknown token hash");
                    return unauthorized("Invalid refresh token.");
                });

        User user = storedToken.getUser();

        if (storedToken.isRevoked()) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for user id={}. Revoking all sessions.", user.getId());
            refreshTokenRepository.revokeAllByUser(user);
            throw unauthorized("Session invalidated. Please log in again.");
        }

        if (storedToken.getExpiredAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw unauthorized("Refresh token has expired. Please log in again.");
        }

        storedToken.setRevoked(true);
        storedToken.setRotatedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        String newRefreshToken = jwtService.generateRefreshToken(String.valueOf(user.getId()));
        saveRefreshToken(user, newRefreshToken);

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String newAccessToken = jwtService.generateAccessToken(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getUsername(),
                roles
        );

        return AuthResult.of(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null) return;

        refreshTokenRepository
                .findByTokenHash(TokenHashUtil.hash(rawRefreshToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    private void saveRefreshToken(User user, String rawRefreshToken) {
        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(TokenHashUtil.hash(rawRefreshToken));
        entity.setUser(user);
        entity.setExpiredAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpired()));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
    }
}
