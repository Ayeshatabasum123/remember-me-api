package com.rememberme.api.communityfund.service;

import com.rememberme.api.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class PrivateDocumentStorageService {

    public static final long DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private final Path privateStorageLocation;
    private final long maxFileSize;

    public PrivateDocumentStorageService(
            @Value("${app.upload.private-dir:uploads/private_documents}") String storageDirProperty,
            @Value("${app.upload.max-document-size:5242880}") long maxFileSize) {
        Path baseDir = Paths.get(storageDirProperty != null && !storageDirProperty.isBlank() ? storageDirProperty : "uploads/private_documents")
                .toAbsolutePath().normalize();
        this.privateStorageLocation = baseDir;
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;

        try {
            Files.createDirectories(this.privateStorageLocation);
            log.info("Initialized private document storage directory: {}", this.privateStorageLocation);
        } catch (IOException e) {
            log.warn("Could not pre-create private storage directory: {}", this.privateStorageLocation);
        }
    }

    public static class StoredDocumentInfo {
        private final String storageReference;
        private final String originalFilename;
        private final String contentType;
        private final long fileSize;
        private final String fileHash;

        public StoredDocumentInfo(String storageReference, String originalFilename, String contentType, long fileSize, String fileHash) {
            this.storageReference = storageReference;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.fileHash = fileHash;
        }

        public String getStorageReference() { return storageReference; }
        public String getOriginalFilename() { return originalFilename; }
        public String getContentType() { return contentType; }
        public long getFileSize() { return fileSize; }
        public String getFileHash() { return fileHash; }
    }

    public StoredDocumentInfo storePrivateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Document file is required and cannot be empty", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > maxFileSize) {
            throw new ApiException("Document file size exceeds maximum allowed limit (5MB)", HttpStatus.BAD_REQUEST);
        }

        String rawFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String originalFilename = StringUtils.cleanPath(rawFilename);
        String extension = getFileExtension(originalFilename);

        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ApiException("Invalid file format. Only PDF, JPG, JPEG, and PNG files are allowed", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException("Invalid file content type: " + contentType + ". Only PDF, JPG, JPEG, and PNG are allowed", HttpStatus.BAD_REQUEST);
        }

        // Validate file content and magic header bytes
        validateDocumentContent(file, extension.toLowerCase());

        // Calculate SHA-256 checksum for duplicate invoice detection
        String fileHash = calculateSha256(file);

        try {
            Files.createDirectories(privateStorageLocation);
            String safeExtension = "." + extension.toLowerCase();
            String uniqueFilename = UUID.randomUUID().toString() + safeExtension;
            Path destinationFile = privateStorageLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Saved private document securely to: {}", destinationFile);
            return new StoredDocumentInfo(destinationFile.toString(), originalFilename, contentType != null ? contentType : "application/octet-stream", file.getSize(), fileHash);
        } catch (IOException e) {
            log.error("Failed to store private document: {}", e.getMessage(), e);
            throw new ApiException("Failed to store uploaded document: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadAsResource(String storageReference) {
        try {
            Path filePath = Paths.get(storageReference).normalize().toAbsolutePath();
            if (!filePath.startsWith(privateStorageLocation)) {
                // Ensure no path traversal outside privateStorageLocation
                throw new ApiException("Invalid document storage path", HttpStatus.BAD_REQUEST);
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ApiException("Document file not found or unreadable on server", HttpStatus.NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            throw new ApiException("Invalid document storage reference", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean deleteDocumentFile(String storageReference) {
        if (storageReference == null || storageReference.isBlank()) {
            return false;
        }
        try {
            Path filePath = Paths.get(storageReference).normalize().toAbsolutePath();
            if (filePath.startsWith(privateStorageLocation)) {
                return Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete document file from disk: {}", storageReference, e);
        }
        return false;
    }

    private void validateDocumentContent(MultipartFile file, String extension) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = inputStream.read(header);
            if (read < 4) {
                throw new ApiException("Uploaded file is corrupted or too small to be a valid document", HttpStatus.BAD_REQUEST);
            }

            boolean isPdf = (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46); // %PDF
            boolean isJpeg = ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF);
            boolean isPng = (read >= 8 &&
                    (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 &&
                    (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47 &&
                    (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A &&
                    (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A);

            if (!isPdf && !isJpeg && !isPng) {
                throw new ApiException("Uploaded file header does not match a valid PDF, JPG, or PNG document", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new ApiException("Failed to read document header: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public String calculateSha256(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("Failed to compute SHA-256 hash for uploaded file: {}", e.getMessage());
            return UUID.randomUUID().toString().replace("-", "");
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
