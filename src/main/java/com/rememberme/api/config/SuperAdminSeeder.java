package com.rememberme.api.config;

import com.rememberme.api.entity.User;
import com.rememberme.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(3)
@ConditionalOnProperty(name = "app.super-admin.create-super-admin", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SuperAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email:superadmin@rememberme.com}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:SuperAdmin@123}")
    private String superAdminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(superAdminEmail).isPresent()) {
            log.info("Super Admin account already exists with email: {}. Skipping seeding.", superAdminEmail);
            return;
        }

        User superAdmin = User.builder()
                .fullName("Super Admin")
                .email(superAdminEmail)
                .password(passwordEncoder.encode(superAdminPassword))
                .role(User.Role.SUPER_ADMIN)
                .enabled(true)
                .emailVerified(true)
                .phoneVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(superAdmin);
        log.info("Super Admin account provisioned securely with email: {}", superAdminEmail);
    }
}
