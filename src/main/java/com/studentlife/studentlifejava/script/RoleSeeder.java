package com.studentlife.studentlifejava.script;

import com.studentlife.studentlifejava.entity.Roles;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.studentlife.studentlifejava.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


// Must run before AdminSeeder (@Order(2)) - AdminSeeder looks up the "admin"
// role by name and just logs a warning and skips seeding if it's missing, so
// getting this ordering wrong doesn't fail loudly, it just silently produces
// no admin user.
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class RoleSeeder implements CommandLineRunner{
    
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Seeding....");

        seedRole("admin");
        seedRole("user");

        log.info("Seed completed.");
    }

    private void seedRole(String name) {
        if (roleRepository.existsByName(name)) {
            log.info("Role already exist skip.");
            return;
        }

        roleRepository.save(Roles.builder().name(name).build());

        log.info("Role seeded successfully.");
    }
}
