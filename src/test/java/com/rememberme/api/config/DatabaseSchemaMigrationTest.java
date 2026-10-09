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
    public void run_ExecutesSchemaAlterations() {
        schemaMigration.run();

        verify(jdbcTemplate, times(1)).execute("UPDATE users SET role = 'ADMIN' WHERE role IN ('SUPER_ADMIN', 'GRAVEYARD_ADMIN')");
        verify(jdbcTemplate, times(1)).execute("UPDATE users SET role = 'USER' WHERE role IS NULL OR role NOT IN ('ADMIN', 'USER')");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        verify(jdbcTemplate, times(1)).execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN'))");
    }

    @Test
    public void run_HandlesExceptionsGracefully() {
        doThrow(new RuntimeException("Database error")).when(jdbcTemplate).execute(anyString());

        // Should log warning and not crash app startup
        schemaMigration.run();

        verify(jdbcTemplate, times(1)).execute(anyString());
    }
}
