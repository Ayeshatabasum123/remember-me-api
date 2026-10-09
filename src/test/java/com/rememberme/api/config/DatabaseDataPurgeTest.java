package com.rememberme.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DatabaseDataPurgeTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseSchemaMigration databaseSchemaMigration;

    @Test
    public void testPurgeSelectedModulesData() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        databaseSchemaMigration.purgeSelectedModuleData();

        // 1. Photos
        verify(jdbcTemplate).execute("DELETE FROM photos");
        // 2. Relationships & Memorials
        verify(jdbcTemplate).execute("DELETE FROM relationships");
        verify(jdbcTemplate).execute("DELETE FROM memorials");
        // 3. Funeral Events
        verify(jdbcTemplate).execute("DELETE FROM funeral_events");
        // 4. Reports
        verify(jdbcTemplate).execute("DELETE FROM reports");
        // 5. Favorites
        verify(jdbcTemplate).execute("DELETE FROM favorites");
        // 6. Deceased Persons
        verify(jdbcTemplate).execute("DELETE FROM deceased_persons");
        // 7. Graves
        verify(jdbcTemplate).execute("DELETE FROM graves");
        // 8. Graveyards
        verify(jdbcTemplate).execute("DELETE FROM graveyards");

        // Users must NOT be deleted
        verify(jdbcTemplate, never()).execute("DELETE FROM users");
    }
}
