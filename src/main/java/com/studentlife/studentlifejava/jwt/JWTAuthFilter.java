package com.studentlife.studentlifejava.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentlife.studentlifejava.dto.response.ApiResponse;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.UserRepository;
import com.studentlife.studentlifejava.utils.CookieUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT processing if the request is already authenticated.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authenticate the request when an access token is present.
        String token = resolveToken(request);
        if (token != null && !authenticateToken(token, request, response)) {
            // authenticateToken already wrote an error response - stop the chain.
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Validates the access token and stores the authenticated user
    // in the Spring Security context.
    // Returns false when an error response has already been written and the
    // filter chain must not continue.
    private boolean authenticateToken(String token,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws IOException {
        try {
            // Extract and validate the user ID from the access token.
            String userId = jwtService.extractUserIdFromAccessToken(token);
            Long id = Long.parseLong(userId);

            // Load the user together with their assigned roles.
            Users user = userRepository.findWithRolesById(id).orElse(null);

            // Reject authentication if the user does not exist or is disabled.
            if (user == null || !user.isEnabled()) {
                log.warn("Rejected token for missing/disabled user id={} [{}]",
                        id, request.getRequestURI());
                return true;
            }

            // Create an authenticated principal with the user's authorities.
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Store the authenticated user for the current request.
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (JwtException e) {
            // Handles expired, invalid, or incorrectly signed tokens.
            log.warn("JWT validation failed [{}]: {}",
                    request.getRequestURI(), e.getMessage());

        } catch (UsernameNotFoundException e) {
            // Handles cases where the token references a missing user.
            log.warn("User not found during token auth [{}]: {}",
                    request.getRequestURI(), e.getMessage());

        } catch (IllegalArgumentException e) {
            // Handles malformed or non-numeric token subjects.
            log.warn("Malformed token subject [{}]: {}",
                    request.getRequestURI(), e.getMessage());

        } catch (DataAccessException e) {
            // A DB outage here is not the client's fault, and this filter runs
            // before DispatcherServlet - so the GlobalException advice never sees
            // anything thrown from it. Letting the chain continue unauthenticated
            // would mislabel the outage as a 401; write an explicit 503 instead.
            log.error("Database unavailable during token auth [{}]",
                    request.getRequestURI(), e);
            writeErrorResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Service temporarily unavailable. Please try again.");
            return false;
        }
        return true;
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                new ApiResponse<>(status, false, message, null));
    }

    // Retrieves the access token from the request cookie.
    // Returns null when the cookie is not present.
    private String resolveToken(HttpServletRequest request) {
        return cookieUtil.getCookieValue(
                request,
                CookieUtil.ACCESS_TOKEN_COOKIE
        );
    }
}
