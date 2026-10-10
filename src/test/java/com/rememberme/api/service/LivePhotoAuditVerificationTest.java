package com.rememberme.api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LivePhotoAuditVerificationTest {

    @Test
    public void testExistingDatabasePhotoUrls() {
        PhotoUrlValidator validator = new PhotoUrlValidator();

        String url1 = "https://commons.wikimedia.org/wiki/Special:FilePath/INDIA%20GATE%20IN%20DELHI.jpg";
        PhotoUrlValidator.ValidationResult res1 = validator.validatePhotoUrl(url1);
        System.out.println("URL 1 Result: valid=" + res1.isValid() + ", resolved=" + res1.getResolvedUrl() + ", error=" + res1.getErrorMessage());
        assertTrue(res1.isValid());
        assertNotNull(res1.getResolvedUrl());

        String url2 = "https://commons.wikimedia.org/wiki/Special:FilePath/Qunu%20Landscape.jpg";
        PhotoUrlValidator.ValidationResult res2 = validator.validatePhotoUrl(url2);
        System.out.println("URL 2 Result: valid=" + res2.isValid() + ", resolved=" + res2.getResolvedUrl() + ", error=" + res2.getErrorMessage());
        assertTrue(res2.isValid());
        assertNotNull(res2.getResolvedUrl());

        String url3 = "https://upload.wikimedia.org/wikipedia/commons/9/93/AbrahamLincoln.jpg?v=00000000000000000000000";
        PhotoUrlValidator.ValidationResult res3 = validator.validatePhotoUrl(url3);
        System.out.println("URL 3 Result: valid=" + res3.isValid() + ", resolved=" + res3.getResolvedUrl() + ", error=" + res3.getErrorMessage());
        assertTrue(res3.isValid());
        assertNotNull(res3.getResolvedUrl());
    }
}
