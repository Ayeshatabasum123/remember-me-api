package com.rememberme.api.communityfund.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when business rules or financial constraints for Community Burial Fund are violated.
 */
public class CommunityFundValidationException extends CommunityFundException {

    public CommunityFundValidationException(String message) {
        super(message, "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);
    }
}
