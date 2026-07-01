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

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        Long id = Long.parseLong(userId);
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
    }
}
