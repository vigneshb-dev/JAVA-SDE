package com.backendlearninglab.spring.dto.response;

import com.backendlearninglab.model.UserRole;
import com.backendlearninglab.spring.entity.UserEntity;

import java.time.Instant;

/**
 * Response DTO for User.
 * <p>
 * SECURITY LESSON:
 * Notice that 'passwordHash' is completely absent!
 * If we returned UserEntity directly, password hashes could easily leak into
 * public API responses and logs.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        String status,
        Instant createdAt
) {
    public static UserResponse fromEntity(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getRole(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
