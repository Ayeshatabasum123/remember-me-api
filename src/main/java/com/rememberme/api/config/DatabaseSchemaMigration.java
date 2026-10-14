package com.rememberme.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Executing database schema migration: Updating user roles and constraints to support SUPER_ADMIN, ADMIN and USER...");
        String[] migrations = {
            "UPDATE users SET role = 'USER' WHERE role IS NULL OR role NOT IN ('SUPER_ADMIN', 'ADMIN', 'USER')",
            "ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check",
            "ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN'))",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS postcode VARCHAR(20)",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS postcode_normalized VARCHAR(20)",
            "CREATE INDEX IF NOT EXISTS idx_users_postcode_normalized ON users(postcode_normalized)",
            "CREATE SEQUENCE IF NOT EXISTS organization_code_seq START WITH 1 INCREMENT BY 1",
            "ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS is_famous boolean NOT NULL DEFAULT false",
            "ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS is_historical boolean NOT NULL DEFAULT false",
            "ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS historical_details text",
            "ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS city VARCHAR(100)",
            "ALTER TABLE graveyards ADD COLUMN IF NOT EXISTS country VARCHAR(100)",
            "UPDATE graveyards SET city = 'Unknown' WHERE city IS NULL OR TRIM(city) = ''",
            "UPDATE graveyards SET country = 'Unknown' WHERE country IS NULL OR TRIM(country) = ''",
            "ALTER TABLE graveyards ALTER COLUMN city TYPE VARCHAR(100)",
            "ALTER TABLE graveyards ALTER COLUMN country TYPE VARCHAR(100)",
            "ALTER TABLE graveyards ALTER COLUMN city SET NOT NULL",
            "ALTER TABLE graveyards ALTER COLUMN country SET NOT NULL",
            "UPDATE users SET postcode_normalized = UPPER(REPLACE(postcode, ' ', '')) WHERE postcode IS NOT NULL AND (postcode_normalized IS NULL OR postcode_normalized = '')",
            "UPDATE organizations SET postcode_normalized = UPPER(REPLACE(postcode, ' ', '')) WHERE postcode IS NOT NULL AND (postcode_normalized IS NULL OR postcode_normalized = '')"
        };

        for (String sql : migrations) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.debug("Schema migration notice for query [{}]: {}", sql, e.getMessage());
            }
        }
        log.info("Successfully checked and updated database schema constraints and columns.");
    }

    /**
     * Permanently deletes all existing data from the 7 target modules:
     * 1. Graveyard (graveyards)
     * 2. Grave (graves)
     * 3. Report (reports)
     * 4. Saved Grave (favorites)
     * 5. Deceased Person (deceased_persons)
     * 6. Media Upload (photos)
     * 7. Memorial (memorials, relationships)
     *
     * All user accounts, roles, profiles, and auth credentials remain preserved.
     */
    @Transactional
    public void purgeSelectedModuleData() {
        try {
            log.info("=== STARTING TARGETED MODULE DATA PURGE ===");
            logTableCounts("BEFORE PURGE");

            // Deletion in strict child -> parent order to respect foreign key constraints:
            // 1. Media Uploads
            jdbcTemplate.execute("DELETE FROM photos");
            // 2. Memorial & Relationships
            jdbcTemplate.execute("DELETE FROM relationships");
            jdbcTemplate.execute("DELETE FROM memorials");
            // 3. Funeral Events referencing deceased persons / graveyards
            jdbcTemplate.execute("DELETE FROM funeral_events");
            // 4. Reports referencing graves
            jdbcTemplate.execute("DELETE FROM reports");
            // 5. Saved Graves referencing graves
            jdbcTemplate.execute("DELETE FROM favorites");
            // 6. Deceased Persons referencing graves
            jdbcTemplate.execute("DELETE FROM deceased_persons");
            // 7. Graves referencing graveyards
            jdbcTemplate.execute("DELETE FROM graves");
            // 8. Graveyards
            jdbcTemplate.execute("DELETE FROM graveyards");

            logTableCounts("AFTER PURGE");
            log.info("=== TARGETED MODULE DATA PURGE COMPLETED SUCCESSFULLY ===");
        } catch (Exception e) {
            log.warn("Notice during targeted module data deletion: {}", e.getMessage());
        }
    }

    private void logTableCounts(String stage) {
        String[] targetTables = {
            "photos", "relationships", "memorials", "funeral_events",
            "reports", "favorites", "deceased_persons", "graves", "graveyards", "users"
        };

        log.info("--- Table Record Counts [{}] ---", stage);
        for (String table : targetTables) {
            try {
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
                log.info("  Table '{}': {} records", table, count != null ? count : 0);
            } catch (Exception e) {
                log.debug("Table '{}' not yet created or inaccessible: {}", table, e.getMessage());
            }
        }
    }
}

