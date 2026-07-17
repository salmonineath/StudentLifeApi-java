package com.studentlife.studentlifejava.service.impl;

import com.studentlife.studentlifejava.dto.AuthResult;
import com.studentlife.studentlifejava.dto.request.AuthRequest;
import com.studentlife.studentlifejava.dto.request.RegisterRequest;
import com.studentlife.studentlifejava.dto.request.ResetPasswordRequest;
import com.studentlife.studentlifejava.dto.response.AuthUserResponse;
import com.studentlife.studentlifejava.entity.RefreshToken;
import com.studentlife.studentlifejava.entity.Roles;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.jwt.JWTService;
import com.studentlife.studentlifejava.mapper.UserMapper;
import com.studentlife.studentlifejava.repository.RefreshTokenRepository;
import com.studentlife.studentlifejava.repository.RoleRepository;
import com.studentlife.studentlifejava.repository.UserRepository;
import com.studentlife.studentlifejava.service.AuthService;
import com.studentlife.studentlifejava.service.VerificationService;
import com.studentlife.studentlifejava.utils.TokenHashUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.*;

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
    private final VerificationService verificationService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()
                || userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw validation("This email or username already been used.");
        }

        Users user = userMapper.toUserEntityRegisterUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Roles defaultRole = roleRepository.findByName("user")
                .orElseThrow(() -> notFound("Default role not found."));
        user.setRoles(new HashSet<>(Set.of(defaultRole)));

        Users savedUser = userRepository.save(user);

        List<String> roles = savedUser.getRoles().stream().map(Roles::getName).toList();

        String accessToken = jwtService.generateAccessToken(
                String.valueOf(savedUser.getId()),
                savedUser.getEmail(),
                savedUser.getUsername(),
                roles
        );
        String refreshToken = jwtService.generateRefreshToken(String.valueOf(savedUser.getId()));

        saveRefreshToken(savedUser, refreshToken);

        AuthUserResponse authUserResponse = userMapper.toAuthUserResponse(savedUser);
        return AuthResult.of(accessToken, refreshToken, authUserResponse);
    }

    @Override
    @Transactional
    public AuthResult login(AuthRequest request) {
        String identifier = request.getEmail_or_username();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );
        } catch (DisabledException e) {
            throw forbidden("Your account has been suspended. Check your email for details");
        } catch (AuthenticationException e) {
            throw unauthorized("Invalid credentials");
        }

        Users user = (Users) authentication.getPrincipal();

        List<String> roles = user.getRoles().stream().map(Roles::getName).toList();

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

        Users user = storedToken.getUser();

        if (storedToken.isRevoked()) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for user id={}. Revoking all sessions.", user.getId());
            refreshTokenRepository.revokeAllByUser(user);
            throw unauthorized("Session invalidated. Please log in again.");
        }

        if (storedToken.getExpiredAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw unauthorized("Refresh token has expired. Please log in again.");
        }

        // Atomic compare-and-set instead of read-then-save: closes the race where
        // two concurrent requests for the same token both pass the isRevoked()
        // check above and both rotate successfully. Losing this update means
        // another request already won the rotation for this exact token.
        int rotated = refreshTokenRepository.revokeIfActive(storedToken.getId());
        if (rotated == 0) {
            log.warn("SECURITY ALERT: Concurrent refresh detected for user id={}. Revoking all sessions.", user.getId());
            refreshTokenRepository.revokeAllByUser(user);
            throw unauthorized("Session invalidated. Please log in again.");
        }
        storedToken.setRotatedAt(Instant.now());

        String newRefreshToken = jwtService.generateRefreshToken(String.valueOf(user.getId()));
        saveRefreshToken(user, newRefreshToken);

        List<String> roles = user.getRoles().stream().map(Roles::getName).toList();
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

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Peek first, consume last: Redis is not part of this JPA transaction,
        // so destroying the token before the password write means any DB failure
        // below leaves the user with a burned token and an unchanged password.
        // Consuming after the writes risks only a token that lingers until its
        // TTL if the delete itself fails - a far cheaper failure.
        String email = verificationService.peekResetToken(request.getResetToken());

        if (email == null) {
            throw unauthorized("Invalid or expired reset token");
        }

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> notFound("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        // A password reset should end every existing session - otherwise a stolen
        // refresh token survives the very action meant to lock the attacker out.
        refreshTokenRepository.revokeAllByUser(user);

        verificationService.consumeResetToken(request.getResetToken());
    }

    private void saveRefreshToken(Users user, String rawRefreshToken) {
        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(TokenHashUtil.hash(rawRefreshToken));
        entity.setUser(user);
        entity.setExpiredAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpired()));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
    }
}
