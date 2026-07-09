package com.studentlife.studentlifejava.script;

import com.studentlife.studentlifejava.entity.Roles;
import com.studentlife.studentlifejava.entity.Users;
import com.studentlife.studentlifejava.repository.RoleRepository;
import com.studentlife.studentlifejava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.admin.username}")
    private String adminUsername;

    @Value("${spring.admin.email}")
    private String adminEmail;

    @Value("${spring.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        log.info("Seed admin...");

        seedAdmin();

        log.info("Admin seed completed.");

    }

    private void seedAdmin() {

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin already exist skip");
            return;
        }

        Roles adminRole = roleRepository.findByName("admin").orElse(null);
        if (adminRole == null) {
            log.warn("Admin role not found. skipping admin seed.");
            return;
        }

        Users admin = Users.builder()
                .fullname("Admin")
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
        log.info("Admin seeded successfully.");

    }
}
