package com.rememberme.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account-file:firebase-service-account.json}")
    private String serviceAccountFile;

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${firebase.project-id:remember-me-eb236}")
    private String projectId;

    @Value("${firebase.enable-mock:false}")
    private boolean enableMock;

    private static boolean mockMode = false;

    public static boolean isMockMode() {
        return mockMode;
    }

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            FirebaseApp existingApp = FirebaseApp.getInstance();
            log.info("Using existing FirebaseApp instance. Resolved Project ID: {}", existingApp.getOptions().getProjectId());
            return existingApp;
        }

        FirebaseApp app = null;

        try {
            InputStream serviceAccountStream = getServiceAccountStream();
            if (serviceAccountStream != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .setProjectId(projectId)
                        .build();
                app = FirebaseApp.initializeApp(options);
                log.info("Successfully initialized FirebaseApp with service account credentials. Resolved Project ID: {}", app.getOptions().getProjectId());
                mockMode = false;
            }
        } catch (Exception e) {
            log.error("Failed to load Firebase service account credentials for project '{}': {}", projectId, e.getMessage());
            if (!enableMock) {
                throw new IllegalStateException("Firebase service account initialization failed for project " + projectId + ": " + e.getMessage(), e);
            }
        }

        if (app == null) {
            try {
                log.info("Attempting FirebaseApp initialization with default Google Application Credentials");
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId(projectId)
                        .build();
                app = FirebaseApp.initializeApp(options);
                log.info("Successfully initialized FirebaseApp with Application Default Credentials. Resolved Project ID: {}", app.getOptions().getProjectId());
                mockMode = false;
            } catch (Exception e) {
                log.warn("Firebase default application credentials not available: {}", e.getMessage());
            }
        }

        if (app != null) {
            String resolvedProjectId = app.getOptions().getProjectId();
            if (StringUtils.hasText(projectId) && !projectId.equalsIgnoreCase(resolvedProjectId)) {
                log.error("CRITICAL: Firebase Project ID mismatch! Configured: '{}', Credentials: '{}'", projectId, resolvedProjectId);
                if (!enableMock) {
                    throw new IllegalStateException(String.format("Firebase Project ID mismatch! Configured: %s, Credentials: %s", projectId, resolvedProjectId));
                }
            }
            return app;
        }

        if (enableMock) {
            log.warn("Firebase credentials missing. Initializing minimal FirebaseApp for EXPLICIT TEST MOCK MODE ONLY (Project ID: {}).", projectId);
            mockMode = true;
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(new MockGoogleCredentials())
                    .setProjectId(projectId)
                    .build();
            return FirebaseApp.initializeApp(options);
        }

        String errorMsg = String.format("Firebase Admin service account credentials are missing for project '%s'. Please set the FIREBASE_SERVICE_ACCOUNT_JSON or FIREBASE_SERVICE_ACCOUNT_FILE environment variable on your deployment.", projectId);
        log.error(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private InputStream getServiceAccountStream() {
        if (StringUtils.hasText(serviceAccountJson)) {
            try {
                log.info("Loading Firebase service account credentials from environment JSON configuration");
                byte[] jsonBytes;
                String trimmed = serviceAccountJson.trim();
                if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    jsonBytes = Base64.getDecoder().decode(trimmed);
                } else {
                    jsonBytes = trimmed.getBytes(StandardCharsets.UTF_8);
                }
                return new ByteArrayInputStream(jsonBytes);
            } catch (Exception e) {
                log.error("Failed to parse firebase.service-account-json content: {}", e.getMessage());
            }
        }

        try {
            Resource classPathResource = new ClassPathResource(serviceAccountFile);
            if (classPathResource.exists()) {
                return classPathResource.getInputStream();
            }
            Resource fileResource = new FileSystemResource(serviceAccountFile);
            if (fileResource.exists()) {
                return fileResource.getInputStream();
            }
        } catch (Exception e) {
            log.debug("Service account file not found on classpath or filesystem: {}", serviceAccountFile);
        }
        return null;
    }

    private static class MockGoogleCredentials extends GoogleCredentials {
        @Override
        public com.google.auth.oauth2.AccessToken refreshAccessToken() {
            return new com.google.auth.oauth2.AccessToken("mock-token", new java.util.Date(System.currentTimeMillis() + 3600000));
        }
    }
}
