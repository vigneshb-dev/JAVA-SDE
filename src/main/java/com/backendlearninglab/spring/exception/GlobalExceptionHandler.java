package com.backendlearninglab.spring.exception;

import com.backendlearninglab.exception.DatabaseOperationException;
import com.backendlearninglab.exception.InsufficientStockException;
import com.backendlearninglab.exception.InvalidOrderException;
import com.backendlearninglab.exception.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for all REST Controllers.
 * <p>
 * TEACHING FOCUS: @RestControllerAdvice & @ExceptionHandler
 * <ul>
 *   <li><b>WHY THIS EXISTS:</b> Eliminates redundant try/catch blocks across 50 controllers.
 *       Controllers simply throw exceptions when invariants are broken, and this centralized
 *       advice intercepts them, logs them, and maps them to appropriate HTTP Status codes.</li>
 *   <li><b>REQUEST LIFECYCLE INTERCEPTION:</b> When a controller method throws an unhandled exception,
 *       DispatcherServlet catches it and routes it to HandlerExceptionResolver, which dispatches
 *       to the matching @ExceptionHandler method in this class!</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 1. 404 NOT FOUND: When a requested entity does not exist.
     */
    @ExceptionHandler({ResourceNotFoundException.class, ProductNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Resource not found: {} at path {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 2. 400 BAD REQUEST: Bean Validation Failures (@Valid).
     * Automatically extracts which fields failed validation and why.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Validation failed for request to {}: {}", request.getRequestURI(), fieldErrors);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input payload failed business validation constraints",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 3. 422 UNPROCESSABLE ENTITY: Business Rule Violations (e.g. Out of Stock).
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        log.warn("Insufficient stock error: {} at {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Insufficient Inventory",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrder(InvalidOrderException ex, HttpServletRequest request) {
        log.warn("Invalid order rejected: {} at {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Order Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 4. 409 CONFLICT: Database unique constraint / referential integrity violations.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Database constraint violation at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Data Integrity Conflict",
                "The requested operation violates database uniqueness or foreign key constraints.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * 5. 401 & 403: Spring Security Exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Invalid credentials or authentication failure: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Access is denied: insufficient role privileges.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * 6. 500 INTERNAL SERVER ERROR: Uncaught exceptions & database infrastructure crashes.
     */
    @ExceptionHandler(DatabaseOperationException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseOperationException(DatabaseOperationException ex, HttpServletRequest request) {
        log.error("Database infrastructure failure at " + request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "A database error occurred while processing your request.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled unexpected exception at " + request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected internal error occurred.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
