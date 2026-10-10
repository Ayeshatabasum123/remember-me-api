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
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int MAX_REDIRECTS = 10;
    private static final String USER_AGENT = "RememberMe-App/1.0 (https://rememberme.org; contact@rememberme.org) Java-HttpsClient/1.0";

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String resolvedUrl;

        private ValidationResult(boolean valid, String errorMessage, String resolvedUrl) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.resolvedUrl = resolvedUrl;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult success(String resolvedUrl) {
            return new ValidationResult(true, null, resolvedUrl);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getResolvedUrl() {
            return resolvedUrl;
        }
    }

    public static class FetchedImage {
        private final byte[] bytes;
        private final String resolvedUrl;
        private final String contentType;

        public FetchedImage(byte[] bytes, String resolvedUrl, String contentType) {
            this.bytes = bytes;
            this.resolvedUrl = resolvedUrl;
            this.contentType = contentType;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getResolvedUrl() {
            return resolvedUrl;
        }

        public String getContentType() {
            return contentType;
        }
    }

    /**
     * Normalizes Wikimedia Commons and Wikipedia photo URLs to direct file paths.
     */
    public String normalizePhotoUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            return photoUrl;
        }
        String cleanUrl = photoUrl.trim().replace(" ", "%20");
        String lower = cleanUrl.toLowerCase();

        // Convert Wikimedia Commons or Wikipedia /wiki/File:Page.jpg to /wiki/Special:FilePath/Page.jpg
        if (lower.contains("wikimedia.org/wiki/file:") || lower.contains("wikipedia.org/wiki/file:")) {
            int fileIdx = lower.indexOf("/wiki/file:") + 11;
            String fileName = cleanUrl.substring(fileIdx);
            if (lower.contains("commons.wikimedia.org")) {
                return "https://commons.wikimedia.org/wiki/Special:FilePath/" + fileName;
            } else if (lower.contains(".wikipedia.org")) {
                int hostEnd = lower.indexOf("/wiki/file:");
                String hostPart = cleanUrl.substring(0, hostEnd);
                return hostPart + "/wiki/Special:FilePath/" + fileName;
            }
        }
        return cleanUrl;
    }

    /**
     * Validates an external photo URL according to security, accessibility, size, and JPEG format rules.
     * Follows HTTP redirects (301, 302, 303, 307, 308) and returns the final resolved image URL upon success.
     */
    public ValidationResult validatePhotoUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            return ValidationResult.success(null);
        }

        String cleanUrl = normalizePhotoUrl(photoUrl);

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
                        || ext.equals(".bmp") || ext.equals(".tiff") || ext.equals(".pdf") || ext.equals(".html")
                        || ext.equals(".htm")) {
                    return ValidationResult.failure("Only JPG and JPEG image URLs are allowed.");
                }
            }

            // 2. SSRF Validation
            if (!isSafeHost(url.getHost())) {
                return ValidationResult.failure("Image URL is not accessible.");
            }

            // 3. Fetch image bytes with safe redirects (301, 302, 303, 307, 308), timeouts, and bounded buffer
            FetchedImage fetched = fetchImageWithLimits(url);
            if (fetched == null || fetched.getBytes() == null) {
                return ValidationResult.failure("Image URL is not accessible.");
            }

            byte[] imageBytes = fetched.getBytes();

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

            return ValidationResult.success(fetched.getResolvedUrl());

        } catch (NonJpgImageException e) {
            return ValidationResult.failure("Only JPG and JPEG image URLs are allowed.");
        } catch (NonImageContentTypeException e) {
            return ValidationResult.failure("Image URL is not accessible.");
        } catch (ImageSizeExceededException e) {
            return ValidationResult.failure("Image size must not exceed 1 MB.");
        } catch (MalformedURLException | IllegalArgumentException e) {
            return ValidationResult.failure("Only JPG and JPEG image URLs are allowed.");
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

    private FetchedImage fetchImageWithLimits(URL initialUrl) throws Exception {
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
            httpsConn.setRequestProperty("User-Agent", USER_AGENT);
            httpsConn.setRequestProperty("Accept", "image/jpeg,image/jpg,image/*,*/*;q=0.8");

            try {
                httpsConn.connect();
                int responseCode = httpsConn.getResponseCode();

                // Explicitly support HTTP 301, 302, 303, 307, and 308 redirects
                boolean isRedirect = (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                        || responseCode == 307
                        || responseCode == 308);

                if (isRedirect) {
                    String location = httpsConn.getHeaderField("Location");
                    if (location == null || location.trim().isEmpty()) {
                        return null;
                    }
                    location = location.trim().replace(" ", "%20");
                    currentUrl = new URL(currentUrl, location);
                    redirects++;
                    continue;
                }

                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    return null;
                }

                // Check content type on final OK response
                String contentType = httpsConn.getContentType();
                if (contentType != null) {
                    String lower = contentType.toLowerCase().trim();
                    if (lower.startsWith("text/html") || lower.startsWith("text/plain") || lower.startsWith("application/json")) {
                        throw new NonImageContentTypeException();
                    }
                    if (lower.startsWith("image/png") || lower.startsWith("image/gif") || lower.startsWith("image/webp")
                            || lower.startsWith("image/svg") || lower.startsWith("image/bmp")) {
                        throw new NonJpgImageException();
                    }
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
                    return new FetchedImage(out.toByteArray(), currentUrl.toString(), contentType);
                }
            } finally {
                httpsConn.disconnect();
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
    public static class NonJpgImageException extends RuntimeException {}
    public static class NonImageContentTypeException extends RuntimeException {}
}
