package com.rememberme.api.communityfund.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user attempts to access or modify a Community Burial Fund resource without permission.
 */
public class CommunityFundAccessDeniedException extends CommunityFundException {

    public CommunityFundAccessDeniedException(String message) {
        super(message, "ACCESS_DENIED", HttpStatus.FORBIDDEN);
    }
}
