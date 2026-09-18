package com.rememberme.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
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

    @Value("${firebase.project-id:}")
    private String projectId;

    @Value("${firebase.enable-mock:false}")
    private boolean enableMock;

    private static boolean mockMode = false;
    private static boolean unconfigured = false;
    private static String unconfiguredReason = null;

    public static boolean isMockMode() {
        return mockMode;
    }

    public static boolean isUnconfigured() {
        return unconfigured;
    }

    public static String getUnconfiguredReason() {
        return unconfiguredReason;
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
                byte[] streamBytes = serviceAccountStream.readAllBytes();
                GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(streamBytes));

                String credentialsProjectId = null;
                if (credentials instanceof ServiceAccountCredentials sac) {
                    credentialsProjectId = sac.getProjectId();
                }

                String effectiveProjectId = StringUtils.hasText(projectId) ? projectId.trim() : null;
                if (StringUtils.hasText(credentialsProjectId)) {
                    if (StringUtils.hasText(effectiveProjectId) && !effectiveProjectId.equalsIgnoreCase(credentialsProjectId)) {
                        log.warn("Configured firebase.project-id ('{}') differs from Service Account project_id ('{}'). Using Service Account project_id '{}'.",
                                effectiveProjectId, credentialsProjectId, credentialsProjectId);
                    }
                    effectiveProjectId = credentialsProjectId;
                }

                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setCredentials(credentials);

                if (StringUtils.hasText(effectiveProjectId)) {
                    optionsBuilder.setProjectId(effectiveProjectId);
                }

                app = FirebaseApp.initializeApp(optionsBuilder.build());
                log.info("Successfully initialized FirebaseApp with service account credentials. Resolved Project ID: {}", app.getOptions().getProjectId());
                mockMode = false;
                unconfigured = false;
                unconfiguredReason = null;
            }
        } catch (Exception e) {
            log.error("Failed to load Firebase service account credentials: {}", e.getMessage(), e);
        }

        if (app == null) {
            try {
                log.info("Attempting FirebaseApp initialization with default Google Application Credentials");
                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault());
                if (StringUtils.hasText(projectId)) {
                    optionsBuilder.setProjectId(projectId.trim());
                }
                app = FirebaseApp.initializeApp(optionsBuilder.build());
                log.info("Successfully initialized FirebaseApp with Application Default Credentials. Resolved Project ID: {}", app.getOptions().getProjectId());
                mockMode = false;
                unconfigured = false;
                unconfiguredReason = null;
            } catch (Exception e) {
                log.warn("Firebase default application credentials not available: {}", e.getMessage());
            }
        }

        String fallbackProjectId = StringUtils.hasText(projectId) ? projectId.trim() : "remember-me-7a323";

        if (app != null) {
            return app;
        }

        if (enableMock) {
            log.warn("Firebase credentials missing. Initializing minimal FirebaseApp for EXPLICIT TEST MOCK MODE ONLY (Project ID: {}).", fallbackProjectId);
            mockMode = true;
            unconfigured = false;
            unconfiguredReason = null;
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(new MockGoogleCredentials())
                    .setProjectId(fallbackProjectId)
                    .build();
            return FirebaseApp.initializeApp(options);
        }

        unconfigured = true;
        unconfiguredReason = "Firebase Admin service account credentials are missing or invalid. Please set the FIREBASE_SERVICE_ACCOUNT_JSON or FIREBASE_SERVICE_ACCOUNT_FILE environment variable on your deployment.";
        log.warn("WARNING: {}", unconfiguredReason);

        FirebaseOptions fallbackOptions = FirebaseOptions.builder()
                .setCredentials(new MockGoogleCredentials())
                .setProjectId(fallbackProjectId)
                .build();
        return FirebaseApp.initializeApp(fallbackOptions);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private InputStream getServiceAccountStream() {
        if (StringUtils.hasText(serviceAccountJson)) {
            try {
                log.info("Loading Firebase service account credentials from environment JSON configuration");
                String trimmed = serviceAccountJson.trim();
                if ((trimmed.startsWith("'") && trimmed.endsWith("'")) || (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
                    trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
                }

                byte[] jsonBytes;
                if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    jsonBytes = Base64.getDecoder().decode(trimmed);
                } else {
                    if (trimmed.contains("\\n") && !trimmed.contains("\n")) {
                        trimmed = trimmed.replace("\\n", "\n");
                    }
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
