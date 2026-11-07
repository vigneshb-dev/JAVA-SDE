package com.backendlearninglab.spring.controller;

import com.backendlearninglab.spring.dto.request.CreateUserRequest;
import com.backendlearninglab.spring.dto.response.ApiResponse;
import com.backendlearninglab.spring.dto.response.UserResponse;
import com.backendlearninglab.spring.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for User Resources.
 * <p>
 * TEACHING FOCUS: Spring MVC Architecture:
 * <ul>
 *   <li><b>@RestController</b>: Convenience annotation combining {@code @Controller} and {@code @ResponseBody}.
 *       Tells Spring that every handler method returns domain objects that Jackson must automatically
 *       serialize to JSON in the HTTP response body!</li>
 *   <li><b>@RequestMapping("/api/v1/users")</b>: Common URI path prefix for all endpoints in this class.</li>
 *   <li><b>@Valid</b>: Triggers Jakarta Bean Validation on the {@code @RequestBody} payload before
 *       entering the method body. If invalid, Spring immediately throws {@code MethodArgumentNotValidException},
 *       routing to {@code GlobalExceptionHandler}!</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    // Constructor Injection
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/v1/users
     * HTTP 201 Created: Used when creating a new resource.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.ok("User created successfully", response);
    }

    /**
     * GET /api/v1/users/{id}
     * Demonstrates @PathVariable to extract values embedded in the URL path.
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ApiResponse.ok(user);
    }

    /**
     * GET /api/v1/users
     * Demonstrates query parameters with optional filtering.
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ApiResponse.ok(users);
    }

    /**
     * PATCH /api/v1/users/{id}/status?newStatus=SUSPENDED
     * HTTP PATCH: Used for partial modification of an existing resource.
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {
        UserResponse updated = userService.updateUserStatus(id, newStatus);
        return ApiResponse.ok("User status updated", updated);
    }

    /**
     * DELETE /api/v1/users/{id}
     * HTTP 204 No Content: Successful deletion with no body returned.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
