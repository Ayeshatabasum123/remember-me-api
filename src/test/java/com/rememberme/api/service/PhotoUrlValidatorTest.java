package com.rememberme.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class PhotoUrlValidatorTest {

    private PhotoUrlValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new PhotoUrlValidator();
    }

    @Test
    public void validatePhotoUrl_NullOrEmpty_Accepted() {
        PhotoUrlValidator.ValidationResult resultNull = validator.validatePhotoUrl(null);
        assertTrue(resultNull.isValid());
        assertNull(resultNull.getErrorMessage());

        PhotoUrlValidator.ValidationResult resultEmpty = validator.validatePhotoUrl("");
        assertTrue(resultEmpty.isValid());

        PhotoUrlValidator.ValidationResult resultWhitespace = validator.validatePhotoUrl("   ");
        assertTrue(resultWhitespace.isValid());
    }

    @Test
    public void validatePhotoUrl_HttpProtocol_Rejected() {
        PhotoUrlValidator.ValidationResult result = validator.validatePhotoUrl("http://example.com/photo.jpg");
        assertFalse(result.isValid());
        assertEquals("Image URL is not accessible.", result.getErrorMessage());
    }

    @Test
    public void validatePhotoUrl_NonJpgExtension_Rejected() {
        PhotoUrlValidator.ValidationResult resultPng = validator.validatePhotoUrl("https://example.com/photo.png");
        assertFalse(resultPng.isValid());
        assertEquals("Only JPG and JPEG image URLs are allowed.", resultPng.getErrorMessage());

        PhotoUrlValidator.ValidationResult resultGif = validator.validatePhotoUrl("https://example.com/photo.gif");
        assertFalse(resultGif.isValid());
        assertEquals("Only JPG and JPEG image URLs are allowed.", resultGif.getErrorMessage());

        PhotoUrlValidator.ValidationResult resultWebp = validator.validatePhotoUrl("https://example.com/photo.webp");
        assertFalse(resultWebp.isValid());
        assertEquals("Only JPG and JPEG image URLs are allowed.", resultWebp.getErrorMessage());
    }

    @Test
    public void validatePhotoUrl_UnsafeHost_SSRF_Rejected() {
        PhotoUrlValidator.ValidationResult localhostRes = validator.validatePhotoUrl("https://localhost/photo.jpg");
        assertFalse(localhostRes.isValid());
        assertEquals("Image URL is not accessible.", localhostRes.getErrorMessage());

        PhotoUrlValidator.ValidationResult loopbackIpRes = validator.validatePhotoUrl("https://127.0.0.1/photo.jpg");
        assertFalse(loopbackIpRes.isValid());
        assertEquals("Image URL is not accessible.", loopbackIpRes.getErrorMessage());

        PhotoUrlValidator.ValidationResult privateIpRes = validator.validatePhotoUrl("https://192.168.1.100/photo.jpg");
        assertFalse(privateIpRes.isValid());
        assertEquals("Image URL is not accessible.", privateIpRes.getErrorMessage());

        PhotoUrlValidator.ValidationResult private10Res = validator.validatePhotoUrl("https://10.0.0.1/photo.jpg");
        assertFalse(private10Res.isValid());
        assertEquals("Image URL is not accessible.", private10Res.getErrorMessage());
    }

    @Test
    public void validatePhotoUrl_InvalidUrl_Rejected() {
        PhotoUrlValidator.ValidationResult result = validator.validatePhotoUrl("not-a-valid-url");
        assertFalse(result.isValid());
        assertEquals("Image URL is not accessible.", result.getErrorMessage());
    }

    @Test
    public void validateImageBytes_ValidJpeg_Accepted() throws IOException {
        byte[] jpegBytes = createSampleJpeg(100, 100);
        PhotoUrlValidator.ValidationResult result = validator.validateImageBytes(jpegBytes);
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void validateImageBytes_Exceeds1MB_Rejected() throws IOException {
        byte[] jpegBytes = createSampleJpeg(10, 10);
        // Create an oversized byte array with JPEG header
        byte[] oversized = new byte[PhotoUrlValidator.MAX_IMAGE_SIZE_BYTES + 10];
        System.arraycopy(jpegBytes, 0, oversized, 0, Math.min(jpegBytes.length, oversized.length));

        PhotoUrlValidator.ValidationResult result = validator.validateImageBytes(oversized);
        assertFalse(result.isValid());
        assertEquals("Image size must not exceed 1 MB.", result.getErrorMessage());
    }

    @Test
    public void validateImageBytes_PngFormat_Rejected() throws IOException {
        byte[] pngBytes = createSamplePng(50, 50);
        PhotoUrlValidator.ValidationResult result = validator.validateImageBytes(pngBytes);
        assertFalse(result.isValid());
        assertEquals("Only JPG and JPEG image URLs are allowed.", result.getErrorMessage());
    }

    @Test
    public void validateImageBytes_CorruptedJpeg_Rejected() {
        // JPEG magic bytes (FF D8 FF E0) followed by corrupted garbage
        byte[] corrupted = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
        PhotoUrlValidator.ValidationResult result = validator.validateImageBytes(corrupted);
        assertFalse(result.isValid());
        assertEquals("Invalid or corrupted JPEG image.", result.getErrorMessage());
    }

    @Test
    public void validateImageBytes_EmptyOrNullBytes_Rejected() {
        PhotoUrlValidator.ValidationResult nullRes = validator.validateImageBytes(null);
        assertFalse(nullRes.isValid());
        assertEquals("Invalid or corrupted JPEG image.", nullRes.getErrorMessage());

        PhotoUrlValidator.ValidationResult emptyRes = validator.validateImageBytes(new byte[0]);
        assertFalse(emptyRes.isValid());
        assertEquals("Invalid or corrupted JPEG image.", emptyRes.getErrorMessage());
    }

    @Test
    public void isSafeHost_BlocksPrivateAndLoopbackAddresses() {
        assertFalse(validator.isSafeHost("localhost"));
        assertFalse(validator.isSafeHost("sub.localhost"));
        assertFalse(validator.isSafeHost("127.0.0.1"));
        assertFalse(validator.isSafeHost("10.0.0.1"));
        assertFalse(validator.isSafeHost("172.16.0.1"));
        assertFalse(validator.isSafeHost("172.31.255.255"));
        assertFalse(validator.isSafeHost("192.168.0.1"));
        assertFalse(validator.isSafeHost("169.254.1.1"));
        assertFalse(validator.isSafeHost("0.0.0.0"));
        assertFalse(validator.isSafeHost(null));
        assertFalse(validator.isSafeHost(""));
    }

    private byte[] createSampleJpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private byte[] createSamplePng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Test
    public void normalizePhotoUrl_TransformsWikimediaFilePage() {
        String input = "https://commons.wikimedia.org/wiki/File:Abraham_Lincoln_Tomb.jpg";
        String normalized = validator.normalizePhotoUrl(input);
        assertEquals("https://commons.wikimedia.org/wiki/Special:FilePath/Abraham_Lincoln_Tomb.jpg", normalized);
    }

    @Test
    public void normalizePhotoUrl_EncodesSpaces() {
        String input = "https://commons.wikimedia.org/wiki/Special:FilePath/INDIA GATE IN DELHI.jpg";
        String normalized = validator.normalizePhotoUrl(input);
        assertEquals("https://commons.wikimedia.org/wiki/Special:FilePath/INDIA%20GATE%20IN%20DELHI.jpg", normalized);
    }

    @Test
    public void validatePhotoUrl_ValidWikimediaCommonsUrl_Accepted() {
        PhotoUrlValidator.ValidationResult result = validator.validatePhotoUrl(
                "https://upload.wikimedia.org/wikipedia/commons/6/6f/Abraham_Lincoln_Tomb.jpg");
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        assertNotNull(result.getResolvedUrl());
    }

    @Test
    public void validatePhotoUrl_SpecialFilePathRedirect_AcceptedAndResolved() {
        PhotoUrlValidator.ValidationResult result = validator.validatePhotoUrl(
                "https://commons.wikimedia.org/wiki/Special:FilePath/Abraham_Lincoln_Tomb.jpg");
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        assertNotNull(result.getResolvedUrl());
        assertTrue(result.getResolvedUrl().contains("upload.wikimedia.org"));
        assertTrue(result.getResolvedUrl().toLowerCase().contains(".jpg"));
    }
}
