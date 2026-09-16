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
            log.info("Executing database schema migration: Updating constraint users_role_check to support ADMIN role...");
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN', 'GRAVEYARD_ADMIN', 'SUPER_ADMIN'))");
            jdbcTemplate.execute("ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS is_famous boolean NOT NULL DEFAULT false");
            jdbcTemplate.execute("ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS is_historical boolean NOT NULL DEFAULT false");
            jdbcTemplate.execute("ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS historical_details text");
            log.info("Successfully updated database schema constraints and graveyard columns.");
        } catch (Exception e) {
            log.warn("Notice: Could not alter users_role_check constraint (may be unsupported in test DB or constraint does not exist): {}", e.getMessage());
        }
    }
}
