package com.rememberme.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Executing database schema migration: Updating user roles and constraints to support ADMIN and USER only...");
            jdbcTemplate.execute("UPDATE users SET role = 'ADMIN' WHERE role IN ('SUPER_ADMIN', 'GRAVEYARD_ADMIN')");
            jdbcTemplate.execute("UPDATE users SET role = 'USER' WHERE role IS NULL OR role NOT IN ('ADMIN', 'USER')");
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN'))");
            jdbcTemplate.execute("ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS is_famous boolean NOT NULL DEFAULT false");
            jdbcTemplate.execute("ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS is_historical boolean NOT NULL DEFAULT false");
            jdbcTemplate.execute("ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS historical_details text");
            log.info("Successfully updated database schema constraints and graveyard columns.");
        } catch (Exception e) {
            log.warn("Notice: Schema migration query notice (may be unsupported in test DB or constraint does not exist): {}", e.getMessage());
        }
    }
}
