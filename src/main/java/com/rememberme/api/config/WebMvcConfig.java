package com.rememberme.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDirProperty;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadDirProperty != null && !uploadDirProperty.isBlank() ? uploadDirProperty : "uploads")
                .toAbsolutePath().normalize();
        Path orgDir = uploadDir.resolve("organizations");

        try {
            Files.createDirectories(uploadDir);
            Files.createDirectories(orgDir);
        } catch (IOException e) {
            // directory creation handled gracefully
        }

        String uploadUri = uploadDir.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }

        String orgUploadUri = orgDir.toUri().toString();
        if (!orgUploadUri.endsWith("/")) {
            orgUploadUri += "/";
        }

        registry.addResourceHandler("/uploads/organizations/**")
                .addResourceLocations(orgUploadUri);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri);
    }

    @Bean
    public RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
