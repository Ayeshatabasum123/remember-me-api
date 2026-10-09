package com.rememberme.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.*;

@Component
@Slf4j
public class PhotoUrlValidator {

    public static final int MAX_IMAGE_SIZE_BYTES = 1024 * 1024; // 1 MB (1,048,576 bytes)
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final int MAX_REDIRECTS = 3;

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Validates an external photo URL according to security, accessibility, size, and JPEG format rules.
     */
    public ValidationResult validatePhotoUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            return ValidationResult.success();
        }

        String cleanUrl = photoUrl.trim();

        // 1. Protocol check: must use HTTPS
        if (!cleanUrl.toLowerCase().startsWith("https://")) {
            String lower = cleanUrl.toLowerCase();
            if (lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg") || lower.endsWith(".bmp")) {
                return ValidationResult.failure("Only JPG and JPEG image URLs are allowed.");
            }
            return ValidationResult.failure("Image URL is not accessible.");
        }

        try {
            URI uri = URI.create(cleanUrl);
            URL url = uri.toURL();

            // Check URL path extension if present (early filter for obvious non-JPGs)
            String path = url.getPath() != null ? url.getPath().toLowerCase() : "";
            if (path.contains(".")) {
                String ext = path.substring(path.lastIndexOf('.'));
                if (ext.equals(".png") || ext.equals(".gif") || ext.equals(".webp") || ext.equals(".svg")
                        || ext.equals(".bmp") || ext.equals(".tiff") || ext.equals(".pdf") || ext.equals(".html")) {
                    return ValidationResult.failure("Only JPG and JPEG image URLs are allowed.");
                }
            }

            // 2. SSRF Validation
            if (!isSafeHost(url.getHost())) {
                return ValidationResult.failure("Image URL is not accessible.");
            }

            // 3. Fetch image bytes with safe redirects, timeouts, and bounded buffer
            byte[] imageBytes = fetchImageBytesWithLimits(url);
            if (imageBytes == null) {
                return ValidationResult.failure("Image URL is not accessible.");
            }

            if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                return ValidationResult.failure("Image size must not exceed 1 MB.");
            }

            // 4. Validate JPEG format and magic bytes
            if (!isJpegFormat(imageBytes)) {
                if (isOtherImageFormat(imageBytes)) {
                    return ValidationResult.failure("Only JPG and JPEG image URLs are allowed.");
                }
                return ValidationResult.failure("Invalid or corrupted JPEG image.");
            }

            // 5. Check if corrupted by decoding via ImageIO
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                BufferedImage image = ImageIO.read(bais);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    return ValidationResult.failure("Invalid or corrupted JPEG image.");
                }
            } catch (Exception e) {
                return ValidationResult.failure("Invalid or corrupted JPEG image.");
            }

            return ValidationResult.success();

        } catch (MalformedURLException | IllegalArgumentException e) {
            return ValidationResult.failure("Only JPG and JPEG image URLs are allowed.");
        } catch (ImageSizeExceededException e) {
            return ValidationResult.failure("Image size must not exceed 1 MB.");
        } catch (Exception e) {
            log.warn("Photo URL validation failed for {}: {}", cleanUrl, e.getMessage());
            return ValidationResult.failure("Image URL is not accessible.");
        }
    }

    /**
     * Validates image bytes directly (useful for testing and content verification).
     */
    public ValidationResult validateImageBytes(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return ValidationResult.failure("Invalid or corrupted JPEG image.");
        }

        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            return ValidationResult.failure("Image size must not exceed 1 MB.");
        }

        if (!isJpegFormat(imageBytes)) {
            if (isOtherImageFormat(imageBytes)) {
                return ValidationResult.failure("Only JPG and JPEG image URLs are allowed.");
            }
            return ValidationResult.failure("Invalid or corrupted JPEG image.");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return ValidationResult.failure("Invalid or corrupted JPEG image.");
            }
        } catch (Exception e) {
            return ValidationResult.failure("Invalid or corrupted JPEG image.");
        }

        return ValidationResult.success();
    }

    public boolean isSafeHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        String lowerHost = host.trim().toLowerCase();
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost")
                || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
            return false;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(lowerHost);
            for (InetAddress addr : addresses) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                        || addr.isAnyLocalAddress() || addr.isMulticastAddress()) {
                    return false;
                }
                byte[] raw = addr.getAddress();
                if (raw.length == 4) {
                    int b0 = raw[0] & 0xFF;
                    int b1 = raw[1] & 0xFF;
                    if (b0 == 10 || (b0 == 172 && b1 >= 16 && b1 <= 31) || (b0 == 192 && b1 == 168)
                            || (b0 == 169 && b1 == 254) || b0 == 127 || b0 == 0) {
                        return false;
                    }
                } else if (raw.length == 16) {
                    // IPv6 loopback (::1) or unique local / link local
                    if ((raw[0] & 0xFE) == 0xFC || (raw[0] == (byte) 0xFE && (raw[1] & 0xC0) == (byte) 0x80)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] fetchImageBytesWithLimits(URL initialUrl) throws Exception {
        URL currentUrl = initialUrl;
        int redirects = 0;

        while (redirects <= MAX_REDIRECTS) {
            if (!currentUrl.getProtocol().equalsIgnoreCase("https")) {
                return null;
            }
            if (!isSafeHost(currentUrl.getHost())) {
                return null;
            }

            URLConnection connection = currentUrl.openConnection();
            if (!(connection instanceof HttpsURLConnection httpsConn)) {
                return null;
            }

            httpsConn.setInstanceFollowRedirects(false);
            httpsConn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            httpsConn.setReadTimeout(READ_TIMEOUT_MS);
            httpsConn.setRequestProperty("User-Agent", "RememberMe-PhotoValidator/1.0");
            httpsConn.setRequestProperty("Accept", "image/jpeg,image/jpg,image/*");

            httpsConn.connect();
            int responseCode = httpsConn.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                String location = httpsConn.getHeaderField("Location");
                if (location == null || location.trim().isEmpty()) {
                    return null;
                }
                currentUrl = new URL(currentUrl, location);
                redirects++;
                continue;
            }

            if (responseCode != HttpsURLConnection.HTTP_OK) {
                return null;
            }

            long contentLength = httpsConn.getContentLengthLong();
            if (contentLength > MAX_IMAGE_SIZE_BYTES) {
                throw new ImageSizeExceededException();
            }

            try (InputStream in = httpsConn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int totalRead = 0;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    totalRead += n;
                    if (totalRead > MAX_IMAGE_SIZE_BYTES) {
                        throw new ImageSizeExceededException();
                    }
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            }
        }
        return null;
    }

    public boolean isJpegFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        // JPEG magic bytes: FF D8 FF
        return (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
    }

    public boolean isOtherImageFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50 && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47) {
            return true;
        }
        // GIF: 47 49 46 38 ("GIF8")
        if ((bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x38) {
            return true;
        }
        // WebP / RIFF
        if (bytes.length >= 12 && (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46) {
            return true;
        }
        // BMP: 42 4D
        if ((bytes[0] & 0xFF) == 0x42 && (bytes[1] & 0xFF) == 0x4D) {
            return true;
        }
        return false;
    }

    public static class ImageSizeExceededException extends RuntimeException {}
}
