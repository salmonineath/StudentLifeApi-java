package com.studentlife.studentlifejava.jwt;

import com.studentlife.studentlifejava.security.UserDetailService;
import com.studentlife.studentlifejava.utils.CookieUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JWTAuthFilter.class);

    private final JWTService jwtService;
    private final CookieUtil cookieUtil;
    private final UserDetailService userDetailService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token != null) {
            authenticateToken(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateToken(String token, HttpServletRequest request) {
        try {
            // Single parse: verifies signature, expiry, and token type atomically
            String userId = jwtService.extractUserIdFromAccessToken(token);

            UserDetails userDetails = userDetailService.loadUserByUsername(userId);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (JwtException e) {
            log.warn("JWT validation failed [{}]: {}", request.getRequestURI(), e.getMessage());
        } catch (UsernameNotFoundException e) {
            log.warn("User not found during token auth [{}]: {}", request.getRequestURI(), e.getMessage());
        } catch (IllegalArgumentException e) {
            // Catches NumberFormatException from Long.parseLong — indicates a tampered token subject
            log.warn("Malformed token subject [{}]: {}", request.getRequestURI(), e.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).strip();
            return token.isEmpty() ? null : token;
        }
        return cookieUtil.getCookieValue(request, "accessToken");
    }
}
