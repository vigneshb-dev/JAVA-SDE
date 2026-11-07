package com.backendlearninglab.spring.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter intercepting incoming requests to extract and validate JWT bearer tokens.
 * <p>
 * TEACHING FOCUS: OncePerRequestFilter & SecurityContextHolder
 * <ul>
 *   <li><b>OncePerRequestFilter</b>: Guarantees that this filter runs exactly once per request,
 *       even during internal asynchronous dispatches or error forwards.</li>
 *   <li><b>Populating SecurityContext</b>: Once a valid token is found, we set an {@link UsernamePasswordAuthenticationToken}
 *       inside the thread's {@link SecurityContextHolder}. Downstream filters (like {@code AuthorizationFilter})
 *       read this object to permit or deny access!</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Check if Authorization header is present and begins with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract raw JWT token string
        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            // 3. If email extracted and user is not already authenticated in this thread
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 4. Validate signature and expiration against user details
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials erased for safety
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. Store authentication token in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Successfully authenticated principal: {} with roles: {}", userEmail, userDetails.getAuthorities());
                }
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed for path {}: {}", request.getRequestURI(), e.getMessage());
            // Do not throw here; let downstream security filters catch missing authentication and return 401
        }

        // 6. Continue filter chain execution
        filterChain.doFilter(request, response);
    }
}
