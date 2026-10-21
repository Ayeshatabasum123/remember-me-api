package com.rememberme.api.communityfund.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested community fund resource cannot be found.
 */
public class CommunityFundNotFoundException extends CommunityFundException {

    public CommunityFundNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s not found with identifier: %s", resourceName, identifier), "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public CommunityFundNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
