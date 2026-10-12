package com.rememberme.api.service;

import com.rememberme.api.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif"
    );

    private final Path uploadLocation = Paths.get("uploads", "organizations");

    public FileStorageService() {
        try {
            Files.createDirectories(uploadLocation);
        } catch (IOException e) {
            log.warn("Could not pre-create directory: {}", uploadLocation.toAbsolutePath());
        }
    }

    public String storeOrganizationPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Organization photograph is required", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException("Photograph file size exceeds maximum allowed limit (10MB)", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String extension = getFileExtension(originalFilename);

        boolean validContentType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
        boolean validExtension = extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());

        if (!validContentType && !validExtension) {
            throw new ApiException("Invalid file type. Only JPEG, PNG, WEBP, and GIF images are allowed", HttpStatus.BAD_REQUEST);
        }

        try {
            Files.createDirectories(uploadLocation);
            String safeExtension = (extension != null && !extension.isBlank()) ? "." + extension.toLowerCase() : ".jpg";
            String savedFilename = UUID.randomUUID().toString() + safeExtension;
            Path destinationFile = uploadLocation.resolve(savedFilename).normalize().toAbsolutePath();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Saved organization photo to: {}", destinationFile);
            return "/uploads/organizations/" + savedFilename;
        } catch (IOException e) {
            log.error("Failed to store organization photo: {}", e.getMessage(), e);
            throw new ApiException("Failed to store uploaded file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return null;
    }
}
