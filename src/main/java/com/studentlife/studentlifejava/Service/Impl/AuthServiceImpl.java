package com.studentlife.studentlifejava.Service.Impl;

import com.studentlife.studentlifejava.DTO.Request.AuthRequest;
import com.studentlife.studentlifejava.DTO.Request.RegisterRequest;
import com.studentlife.studentlifejava.DTO.Response.ApiResponse;
import com.studentlife.studentlifejava.DTO.Response.AuthResponse;
import com.studentlife.studentlifejava.DTO.Response.UserResponse;
import com.studentlife.studentlifejava.Entity.Role;
import com.studentlife.studentlifejava.Entity.User;
import com.studentlife.studentlifejava.JWT.JWTService;
import com.studentlife.studentlifejava.Mapper.UserMapper;
import com.studentlife.studentlifejava.Repository.RoleRepository;
import com.studentlife.studentlifejava.Repository.UserRepository;
import com.studentlife.studentlifejava.Service.AuthService;
import com.studentlife.studentlifejava.Utils.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.studentlife.studentlifejava.Exception.ErrorsExceptionFactory.notFound;
import static com.studentlife.studentlifejava.Exception.ErrorsExceptionFactory.validation;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final CookieUtil cookieUtil;

    @Override
    public ApiResponse<?> register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.findByEmail(request.getEmail()).isPresent() || userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw validation("This email or username already been used.");
        }

        User user = userMapper.toUserEntityRegisterUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role defaultRole = roleRepository.findByName("student")
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

//        Cookie cookie = new Cookie("access_token", accessToken);
//        cookie.setHttpOnly(true);
//        cookie.setPath("/");
//        cookie.setMaxAge(60 * 60 * 24);
//        response.addCookie(cookie);
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
}
