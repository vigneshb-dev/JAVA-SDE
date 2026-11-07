package com.backendlearninglab.spring.controller;

import com.backendlearninglab.spring.dto.request.CreateUserRequest;
import com.backendlearninglab.spring.dto.request.LoginRequest;
import com.backendlearninglab.spring.dto.response.ApiResponse;
import com.backendlearninglab.spring.dto.response.AuthResponse;
import com.backendlearninglab.spring.dto.response.UserResponse;
import com.backendlearninglab.spring.security.CustomUserPrincipal;
import com.backendlearninglab.spring.security.JwtService;
import com.backendlearninglab.spring.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handling user registration, authentication, and JWT issuance.
 * <p>
 * TEACHING FOCUS: The Authentication Flow:
 * <ol>
 *   <li>Client submits JSON with email and plaintext password.</li>
 *   <li>{@link AuthenticationManager} invokes DaoAuthenticationProvider.</li>
 *   <li>Provider calls {@code userDetailsService.loadUserByUsername(email)} and compares passwords using BCrypt.</li>
 *   <li>If matched, {@link JwtService} signs an HMAC-SHA256 JWT containing identity claims and returns it.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        log.info("Received public registration request for: {}", request.email());
        UserResponse response = userService.createUser(request);
        return ApiResponse.ok("User registered successfully. You may now log in.", response);
    }

    /**
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Processing login for: {}", request.email());

        // 1. Authenticate credentials via Spring Security's AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        // 2. Extract authenticated principal
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        // 3. Issue cryptographically signed JWT
        String token = jwtService.generateToken(
                principal,
                principal.getId(),
                principal.getUserEntity().getRole().name()
        );

        UserResponse userResponse = UserResponse.fromEntity(principal.getUserEntity());
        AuthResponse authResponse = AuthResponse.of(token, 86400, userResponse);

        log.info("Issued JWT for principal: {} (Role: {})", principal.getUsername(), principal.getUserEntity().getRole());
        return ApiResponse.ok("Login successful", authResponse);
    }

    /**
     * GET /api/v1/auth/me
     * Demonstrates extracting currently authenticated principal via @AuthenticationPrincipal.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated principal in SecurityContext");
        }
        return ApiResponse.ok(UserResponse.fromEntity(principal.getUserEntity()));
    }
}
