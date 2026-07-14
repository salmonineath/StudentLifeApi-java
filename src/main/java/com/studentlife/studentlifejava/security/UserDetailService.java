package com.studentlife.studentlifejava.security;

import com.studentlife.studentlifejava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    // The UserDetailsService contract names this parameter "username", but this
    // app authenticates by numeric user id (the JWT subject) - not a real username.
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        // JWTAuthFilter specifically catches IllegalArgumentException around this
        // call to handle a tampered/malformed token subject as a clean
        // "unauthenticated" outcome rather than a 500. If this ever switches to a
        // non-numeric id type, that catch block needs to change too.
        Long id = Long.parseLong(userId);
        return userRepository.findWithRolesById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
    }
}
