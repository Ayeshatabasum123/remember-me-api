package com.rememberme.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationCodeGeneratorService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public synchronized String generateNextCode() {
        try {
            Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('organization_code_seq')", Long.class);
            if (nextVal == null) {
                nextVal = 1L;
            }
            return String.format("ORG-%06d", nextVal);
        } catch (Exception e) {
            log.warn("Sequence organization_code_seq not ready, attempting creation: {}", e.getMessage());
            try {
                jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS organization_code_seq START WITH 1 INCREMENT BY 1");
                Long nextVal = jdbcTemplate.queryForObject("SELECT nextval('organization_code_seq')", Long.class);
                return String.format("ORG-%06d", nextVal != null ? nextVal : 1L);
            } catch (Exception ex) {
                log.error("Failed to query database sequence: {}", ex.getMessage());
                // Fallback timestamp-based code
                return String.format("ORG-%06d", System.currentTimeMillis() % 1000000);
            }
        }
    }
}
