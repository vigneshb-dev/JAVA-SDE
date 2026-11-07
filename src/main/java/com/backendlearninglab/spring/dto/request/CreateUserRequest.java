package com.backendlearninglab.spring.dto.request;

import com.backendlearninglab.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new User.
 * <p>
 * TEACHING FOCUS: Jakarta Bean Validation
 * <ul>
 *   <li><b>@NotBlank</b>: Validates that string is non-null and contains at least one non-whitespace character.</li>
 *   <li><b>@Email</b>: Enforces RFC 5322 compliant email format.</li>
 *   <li><b>@Size</b>: Enforces minimum and maximum length bounds.</li>
 * </ul>
 */
public record CreateUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        UserRole role
) {}
