package com.rememberme.api.service;

import com.rememberme.api.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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

    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png"
    );

    private final Path uploadLocation;

    public FileStorageService() {
        this("uploads");
    }

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDirProperty) {
        Path baseUploadDir = Paths.get(uploadDirProperty != null && !uploadDirProperty.isBlank() ? uploadDirProperty : "uploads")
                .toAbsolutePath().normalize();
        this.uploadLocation = baseUploadDir.resolve("organizations");
        try {
            Files.createDirectories(this.uploadLocation);
            log.info("Initialized organization photo upload directory: {}", this.uploadLocation);
        } catch (IOException e) {
            log.warn("Could not pre-create directory: {}", this.uploadLocation);
        }
    }

    public Path getUploadLocation() {
        return this.uploadLocation;
    }

    public String storeOrganizationPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Organization photograph is required", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException("Photograph file size exceeds maximum allowed limit (5MB)", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String extension = getFileExtension(originalFilename);

        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ApiException("Invalid image format. Only JPG, JPEG, and PNG images are allowed", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException("Invalid image format. Only JPG, JPEG, and PNG images are allowed", HttpStatus.BAD_REQUEST);
        }

        // Validate actual image content, not only file extensions
        validateImageContent(file);

        try {
            Files.createDirectories(uploadLocation);
            String safeExtension = "." + extension.toLowerCase();
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

    private void validateImageContent(MultipartFile file) {
        // 1. Verify magic header bytes for JPG or PNG
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = inputStream.read(header);
            if (read < 3) {
                throw new ApiException("Invalid image format. Uploaded file does not contain valid image content", HttpStatus.BAD_REQUEST);
            }

            boolean isJpeg = ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF);
            boolean isPng = (read >= 8 &&
                    (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 &&
                    (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47 &&
                    (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A &&
                    (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A);

            if (!isJpeg && !isPng) {
                throw new ApiException("Invalid image format. Only JPG, JPEG, and PNG images are allowed", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new ApiException("Failed to read photograph content: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // 2. Verify image decoding using ImageIO
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new ApiException("Invalid image format. Uploaded file could not be decoded as an image", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new ApiException("Failed to decode photograph content: " + e.getMessage(), HttpStatus.BAD_REQUEST);
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
