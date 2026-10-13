package com.rememberme.api.service;

import com.rememberme.api.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @BeforeEach
    public void setUp() {
        fileStorageService = new FileStorageService();
    }

    private byte[] createValidImageBytes(String format) {
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, format, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate test image", e);
        }
    }

    @Test
    public void storeOrganizationPhoto_ValidJpeg_Success() {
        byte[] jpegBytes = createValidImageBytes("jpeg");
        MockMultipartFile file = new MockMultipartFile("photograph", "church.jpg", "image/jpeg", jpegBytes);

        String result = fileStorageService.storeOrganizationPhoto(file);

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/organizations/"));
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    public void storeOrganizationPhoto_ValidPng_Success() {
        byte[] pngBytes = createValidImageBytes("png");
        MockMultipartFile file = new MockMultipartFile("photograph", "church.png", "image/png", pngBytes);

        String result = fileStorageService.storeOrganizationPhoto(file);

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/organizations/"));
        assertTrue(result.endsWith(".png"));
    }

    @Test
    public void storeOrganizationPhoto_NullFile_ThrowsBadRequest() {
        ApiException ex = assertThrows(ApiException.class, () ->
                fileStorageService.storeOrganizationPhoto(null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Organization photograph is required", ex.getMessage());
    }

    @Test
    public void storeOrganizationPhoto_EmptyFile_ThrowsBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile("photograph", "empty.jpg", "image/jpeg", new byte[0]);

        ApiException ex = assertThrows(ApiException.class, () ->
                fileStorageService.storeOrganizationPhoto(emptyFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Organization photograph is required", ex.getMessage());
    }

    @Test
    public void storeOrganizationPhoto_FileExceeds5MB_ThrowsBadRequest() {
        byte[] largeBytes = new byte[(int) (FileStorageService.MAX_FILE_SIZE + 1024)];
        MockMultipartFile largeFile = new MockMultipartFile("photograph", "large.jpg", "image/jpeg", largeBytes);

        ApiException ex = assertThrows(ApiException.class, () ->
                fileStorageService.storeOrganizationPhoto(largeFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Photograph file size exceeds maximum allowed limit (5MB)", ex.getMessage());
    }

    @Test
    public void storeOrganizationPhoto_InvalidExtension_ThrowsBadRequest() {
        MockMultipartFile txtFile = new MockMultipartFile("photograph", "document.txt", "text/plain", "hello".getBytes());

        ApiException ex = assertThrows(ApiException.class, () ->
                fileStorageService.storeOrganizationPhoto(txtFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Only JPG, JPEG, and PNG images are allowed"));
    }

    @Test
    public void storeOrganizationPhoto_WebpRejected_ThrowsBadRequest() {
        MockMultipartFile webpFile = new MockMultipartFile("photograph", "image.webp", "image/webp", "dummy".getBytes());

        ApiException ex = assertThrows(ApiException.class, () ->
                fileStorageService.storeOrganizationPhoto(webpFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Only JPG, JPEG, and PNG images are allowed"));
    }

    @Test
    public void storeOrganizationPhoto_FakeJpgWithTextContent_ThrowsBadRequest() {
        MockMultipartFile fakeFile = new MockMultipartFile("photograph", "fake.jpg", "image/jpeg", "not a real image".getBytes());

        ApiException ex = assertThrows(ApiException.class, () ->
                fileStorageService.storeOrganizationPhoto(fakeFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid image format"));
    }
}
