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
            "UPDATE organizations SET postcode_normalized = UPPER(REPLACE(postcode, ' ', '')) WHERE postcode IS NOT NULL AND (postcode_normalized IS NULL OR postcode_normalized = '')",
            "ALTER TABLE graves ALTER COLUMN grave_number TYPE VARCHAR(100) USING grave_number::VARCHAR",
            "ALTER TABLE graves ADD COLUMN IF NOT EXISTS postcode VARCHAR(20)",
            "ALTER TABLE graves ALTER COLUMN postcode TYPE VARCHAR(20)",
            "CREATE TABLE IF NOT EXISTS donations (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "user_id BIGINT NOT NULL REFERENCES users(id), " +
                "amount_pence BIGINT NOT NULL, " +
                "currency VARCHAR(3) NOT NULL DEFAULT 'GBP', " +
                "payment_status VARCHAR(32) NOT NULL DEFAULT 'PENDING', " +
                "settlement_status VARCHAR(32) NOT NULL DEFAULT 'PENDING', " +
                "fee_pence BIGINT NOT NULL DEFAULT 0, " +
                "net_amount_pence BIGINT NOT NULL DEFAULT 0, " +
                "refunded_amount_pence BIGINT NOT NULL DEFAULT 0, " +
                "stripe_payment_intent_id VARCHAR(255) UNIQUE, " +
                "stripe_charge_id VARCHAR(255), " +
                "stripe_balance_transaction_id VARCHAR(255), " +
                "stripe_customer_id VARCHAR(255), " +
                "idempotency_key VARCHAR(255) UNIQUE, " +
                "client_secret VARCHAR(512), " +
                "donor_email VARCHAR(255), " +
                "donor_name VARCHAR(255), " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "succeeded_at TIMESTAMP, " +
                "settled_at TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS donation_dedications (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "donation_id BIGINT NOT NULL REFERENCES donations(id) ON DELETE CASCADE, " +
                "dedication_name VARCHAR(255) NOT NULL, " +
                "deceased_person_id BIGINT, " +
                "memorial_id BIGINT, " +
                "message TEXT, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS payment_events (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "stripe_event_id VARCHAR(255) NOT NULL UNIQUE, " +
                "event_type VARCHAR(100) NOT NULL, " +
                "stripe_payment_intent_id VARCHAR(255), " +
                "stripe_charge_id VARCHAR(255), " +
                "status VARCHAR(32) NOT NULL DEFAULT 'PROCESSED', " +
                "payload TEXT, " +
                "error_message TEXT, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS donation_receipts (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "donation_id BIGINT NOT NULL UNIQUE REFERENCES donations(id) ON DELETE CASCADE, " +
                "receipt_number VARCHAR(64) NOT NULL UNIQUE, " +
                "donor_name VARCHAR(255) NOT NULL, " +
                "donor_email VARCHAR(255) NOT NULL, " +
                "amount_pence BIGINT NOT NULL, " +
                "currency VARCHAR(3) NOT NULL DEFAULT 'GBP', " +
                "payment_method VARCHAR(50), " +
                "stripe_charge_id VARCHAR(255), " +
                "fund_name VARCHAR(255) NOT NULL DEFAULT 'Community Burial Fund', " +
                "receipt_url VARCHAR(512), " +
                "issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS fund_ledger (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "donation_id BIGINT, " +
                "entry_type VARCHAR(50) NOT NULL, " +
                "gross_amount_pence BIGINT NOT NULL, " +
                "fee_pence BIGINT NOT NULL DEFAULT 0, " +
                "net_amount_pence BIGINT NOT NULL, " +
                "currency VARCHAR(3) NOT NULL DEFAULT 'GBP', " +
                "running_balance_pence BIGINT NOT NULL, " +
                "stripe_balance_transaction_id VARCHAR(255), " +
                "reference VARCHAR(255) NOT NULL, " +
                "description TEXT NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
            "CREATE INDEX IF NOT EXISTS idx_donations_user_id ON donations(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_donations_payment_status ON donations(payment_status)",
            "CREATE INDEX IF NOT EXISTS idx_donations_settlement_status ON donations(settlement_status)",
            "CREATE INDEX IF NOT EXISTS idx_donation_dedications_donation_id ON donation_dedications(donation_id)",
            "CREATE INDEX IF NOT EXISTS idx_payment_events_type ON payment_events(event_type)",
            "CREATE INDEX IF NOT EXISTS idx_fund_ledger_entry_type ON fund_ledger(entry_type)",
            "CREATE INDEX IF NOT EXISTS idx_fund_ledger_created_at ON fund_ledger(created_at)",
            "CREATE TABLE IF NOT EXISTS fund_settings (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "fund_name VARCHAR(255) NOT NULL, " +
                "introduction TEXT, " +
                "purpose TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "donation_information TEXT, " +
                "privacy_information TEXT NOT NULL, " +
                "funding_disclaimer TEXT NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
            "INSERT INTO fund_settings (fund_name, introduction, purpose, description, donation_information, privacy_information, funding_disclaimer, created_at, updated_at) " +
                "SELECT 'Remember Me Community Burial Fund', " +
                "'Providing compassionate, communal burial assistance to families facing financial hardship.', " +
                "'To relieve burial and funeral poverty by providing direct, discreet financial assistance to verified burial service providers on behalf of bereaved families in need.', " +
                "'The Remember Me Community Burial Fund is a collective community initiative dedicated to ensuring every individual receives a dignified burial regardless of financial hardship. All contributions flow directly into a central, strictly audited communal fund.', " +
                "'Donations of any amount are welcomed via card, Apple Pay, and Google Pay. You may dedicate your contribution in loving memory of deceased relatives, which serves as a tribute and prayer.', " +
                "'Strict applicant privacy is guaranteed by policy and system architecture. Financial hardship cases, family identities, applicant names, and documents are kept strictly confidential and are never published on memorials, public grave registries, or public impact dashboards.', " +
                "'Financial assistance is subject to fund availability, independent verification of burial costs, and two-person administrative approval. An application does not guarantee funding.', " +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP " +
                "WHERE NOT EXISTS (SELECT 1 FROM fund_settings)",
            "ALTER TABLE donation_dedications ADD COLUMN IF NOT EXISTS loved_one_name VARCHAR(255)",
            "ALTER TABLE donation_dedications ADD COLUMN IF NOT EXISTS dedication_message TEXT",
            "UPDATE donation_dedications SET loved_one_name = dedication_name WHERE loved_one_name IS NULL",
            "UPDATE donation_dedications SET dedication_message = message WHERE dedication_message IS NULL",
            "ALTER TABLE donations ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(50) DEFAULT 'STRIPE'",
            "ALTER TABLE donations ADD COLUMN IF NOT EXISTS provider_payment_id VARCHAR(255)",
            "UPDATE donations SET provider_payment_id = stripe_payment_intent_id WHERE provider_payment_id IS NULL",
            "CREATE TABLE IF NOT EXISTS payment_transactions (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "donation_id BIGINT NOT NULL REFERENCES donations(id) ON DELETE CASCADE, " +
                "provider_transaction_id VARCHAR(255), " +
                "amount NUMERIC(12, 2) NOT NULL, " +
                "amount_pence BIGINT NOT NULL, " +
                "currency VARCHAR(3) NOT NULL DEFAULT 'GBP', " +
                "status VARCHAR(32) NOT NULL, " +
                "transaction_type VARCHAR(50) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
            "CREATE INDEX IF NOT EXISTS idx_payment_transactions_donation_id ON payment_transactions(donation_id)",
            "CREATE INDEX IF NOT EXISTS idx_payment_transactions_provider_id ON payment_transactions(provider_transaction_id)"
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

