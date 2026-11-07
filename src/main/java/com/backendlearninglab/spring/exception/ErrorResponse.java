package com.backendlearninglab.spring.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized API Error Response Body.
 * <p>
 * TEACHING FOCUS: Consistent API Error Contract.
 * Every client (React, iOS, Android, partner API) receives the exact same JSON format
 * whenever an error occurs anywhere across the system.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> validationErrors) {
        this(Instant.now(), status, error, message, path, validationErrors);
    }
}
