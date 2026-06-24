package com.studentlife.studentlifejava.script;

import com.studentlife.studentlifejava.Entity.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.studentlife.studentlifejava.Repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
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

        roleRepository.save(Role.builder().name(name).build());

        log.info("Role seeded successfully.");
    }
}
