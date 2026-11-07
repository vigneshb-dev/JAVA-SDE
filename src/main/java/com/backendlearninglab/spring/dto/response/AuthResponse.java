package com.backendlearninglab.spring.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
    public static AuthResponse of(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
