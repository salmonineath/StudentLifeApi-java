package com.studentlife.studentlifejava.utils;

import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.jwt.JWTService;
import com.studentlife.studentlifejava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;

import static com.studentlife.studentlifejava.exception.ErrorsExceptionFactory.unauthorized;

@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final JWTService jwtService;
    private final UserRepository userRepository;

    public Users getAuthenticatedUser() {
        return jwtService.getCurrentUser();
    }

    public Long getUserIdFormPrincipal(Principal principal) {

        if (principal == null) {
            throw unauthorized("Not authenticated");
        }

        if (principal instanceof UsernamePasswordAuthenticationToken auth &&
        auth.getPrincipal() instanceof Users users) {
            return users.getId();
        }

        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> unauthorized("User not found"))
                .getId();
    }
}
