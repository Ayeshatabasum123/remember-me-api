package com.rememberme.api.config;

import com.rememberme.api.exception.ApiException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Converter that prevents Spring Boot from attempting to convert String inputs
 * (such as Swagger UI string fields or string form-data) into a MultipartFile.
 * Returns null for empty/blank strings so mandatory file validation can trigger,
 * or throws a clear ApiException when an actual string value is passed.
 */
@Component
public class StringToMultipartFileConverter implements Converter<String, MultipartFile> {

    @Override
    public MultipartFile convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        throw new ApiException(
                "Organization photograph must be uploaded as a valid image file (JPG, JPEG, PNG), not sent as a String",
                HttpStatus.BAD_REQUEST
        );
    }
}
