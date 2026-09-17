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

import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account-file:firebase-service-account.json}")
    private String serviceAccountFile;

    @Value("${firebase.project-id:remember-me-eb236}")
    private String projectId;

    private static boolean mockMode = false;

    public static boolean isMockMode() {
        return mockMode;
    }

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            InputStream serviceAccountStream = getServiceAccountStream();
            if (serviceAccountStream != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                        .build();
                log.info("Initializing FirebaseApp with service account credentials from {}", serviceAccountFile);
                mockMode = false;
                return FirebaseApp.initializeApp(options);
            }
        } catch (Exception e) {
            log.warn("Could not load Firebase service account file '{}': {}. Falling back to default app initialization.",
                    serviceAccountFile, e.getMessage());
        }

        try {
            log.info("Attempting FirebaseApp initialization with default Google Credentials");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
            mockMode = false;
            return FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            log.warn("Firebase default credentials not available: {}. Initializing minimal FirebaseApp for development (Project ID: {}).", e.getMessage(), projectId);
            mockMode = true;
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(new MockGoogleCredentials())
                    .setProjectId(projectId)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private InputStream getServiceAccountStream() {
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
