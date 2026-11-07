package com.backendlearninglab.spring.security;

import com.backendlearninglab.spring.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles HTTP 403 Forbidden.
 * <p>
 * Invoked when an authenticated user attempts to access a resource for which
 * their assigned role or authorities are insufficient (e.g. USER attempting to delete a product).
 */
@Component
public class DelegatedAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public DelegatedAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Access Denied: You do not possess the required role/authority for this endpoint.",
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
