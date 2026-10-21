package com.rememberme.api.communityfund.exception;

import com.rememberme.api.exception.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base domain exception for Community Burial Fund operations.
 */
@Getter
public class CommunityFundException extends ApiException {

    private final String errorCode;

    public CommunityFundException(String message, HttpStatus status) {
        super(message, status);
        this.errorCode = status.name();
    }

    public CommunityFundException(String message, String errorCode, HttpStatus status) {
        super(message, status);
        this.errorCode = errorCode;
    }
}
