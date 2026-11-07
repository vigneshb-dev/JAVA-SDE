package com.backendlearninglab.spring.dto.response;

import java.time.Instant;

/**
 * Standardized API Response Wrapper.
 * Provides consistent envelope formatting across all successful responses.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("Operation completed successfully", data);
    }
}
