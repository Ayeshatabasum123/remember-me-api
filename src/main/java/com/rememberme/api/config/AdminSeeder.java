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
@Order(2)
@ConditionalOnProperty(name = "app.admin.create-test-admin", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin-test@example.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin test user already exists with email: {}. Skipping seeding.", adminEmail);
            return;
        }

        User admin = User.builder()
                .fullName("Admin Test")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(User.Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .phoneVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(admin);
        log.info("Admin test user created successfully with email: {}", adminEmail);
    }
}
