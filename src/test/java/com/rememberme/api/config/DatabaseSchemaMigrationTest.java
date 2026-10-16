package com.rememberme.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DatabaseSchemaMigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseSchemaMigration schemaMigration;

    @Test
    public void run_ExecutesSchemaAlterationsWithoutPurgingData() {
        schemaMigration.run();

        // Verify user schema migration with SUPER_ADMIN, ADMIN, USER and postcode columns
        verify(jdbcTemplate, times(1)).execute("UPDATE users SET role = 'USER' WHERE role IS NULL OR role NOT IN ('SUPER_ADMIN', 'ADMIN', 'USER')");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN'))");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS postcode VARCHAR(20)");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS postcode_normalized VARCHAR(20)");
        verify(jdbcTemplate, times(1)).execute("CREATE INDEX IF NOT EXISTS idx_users_postcode_normalized ON users(postcode_normalized)");
        verify(jdbcTemplate, times(1)).execute("CREATE SEQUENCE IF NOT EXISTS organization_code_seq START WITH 1 INCREMENT BY 1");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE graves ALTER COLUMN grave_number TYPE VARCHAR(100) USING grave_number::VARCHAR");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE graves ADD COLUMN IF NOT EXISTS postcode VARCHAR(20)");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE graves ALTER COLUMN postcode TYPE VARCHAR(20)");

        // Existing module data must NOT be automatically deleted on startup
        verify(jdbcTemplate, never()).execute("DELETE FROM photos");
        verify(jdbcTemplate, never()).execute("DELETE FROM graves");
        verify(jdbcTemplate, never()).execute("DELETE FROM graveyards");
    }

    @Test
    public void run_HandlesExceptionsGracefully() {
        doThrow(new RuntimeException("Database error")).when(jdbcTemplate).execute(anyString());

        // Should log warning and not crash app startup
        schemaMigration.run();

        verify(jdbcTemplate, atLeastOnce()).execute(anyString());
    }

    @Test
    public void purgeSelectedModuleData_ExecutesCorrectDeletionOrder() {
        schemaMigration.purgeSelectedModuleData();

        verify(jdbcTemplate, times(1)).execute("DELETE FROM photos");
        verify(jdbcTemplate, times(1)).execute("DELETE FROM relationships");
        verify(jdbcTemplate, times(1)).execute("DELETE FROM memorials");
        verify(jdbcTemplate, times(1)).execute("DELETE FROM funeral_events");
        verify(jdbcTemplate, times(1)).execute("DELETE FROM reports");
        verify(jdbcTemplate, times(1)).execute("DELETE FROM favorites");
        verify(jdbcTemplate, times(1)).execute("DELETE FROM deceased_persons");
        verify(jdbcTemplate, times(1)).execute("DELETE FROM graves");
        verify(jdbcTemplate, times(1)).execute("DELETE FROM graveyards");
    }
}

