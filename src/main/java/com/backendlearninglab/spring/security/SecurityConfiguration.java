package com.backendlearninglab.spring.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Master Spring Security 6 Configuration.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li><b>@EnableWebSecurity</b>: Activates Spring Security's web filter chain integration.</li>
 *   <li><b>@EnableMethodSecurity</b>: Enables fine-grained method-level security annotations like
 *       {@code @PreAuthorize("hasRole('ADMIN')")} directly on service or controller methods.</li>
 *   <li><b>Stateless Architecture</b>: Disables HTTP Sessions and CSRF protection; authenticates
 *       strictly on every request via cryptographic JWT verification.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final DelegatedAuthenticationEntryPoint authenticationEntryPoint;
    private final DelegatedAccessDeniedHandler accessDeniedHandler;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthFilter,
                                 CustomUserDetailsService userDetailsService,
                                 DelegatedAuthenticationEntryPoint authenticationEntryPoint,
                                 DelegatedAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Cross-Site Request Forgery) because we are stateless JWT API
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Configure URL-level Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints: Authentication registration, login, and health check
                        .requestMatchers("/api/v1/auth/**", "/api/v1/health").permitAll()

                        // Public product browsing
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                        // Catalog management: Restricted to ADMIN and INVENTORY_MANAGER
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                        // Warehouse inventory management: Restricted to ADMIN and INVENTORY_MANAGER
                        .requestMatchers("/api/v1/inventory/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")

                        // User administrative management: Restricted strictly to ADMIN
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        // Customer checkout: Requires any authenticated user (USER, ADMIN, etc.)
                        .requestMatchers("/api/v1/orders/**").authenticated()

                        // All other endpoints require authentication by default (Secure by default principle)
                        .anyRequest().authenticated()
                )

                // 3. Configure Stateless Session Management (No JSESSIONID cookies!)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Register Custom 401 & 403 Exception Entry Points
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // 5. Register Authentication Provider
                .authenticationProvider(authenticationProvider())

                // 6. Insert JwtAuthenticationFilter before Spring's UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures DaoAuthenticationProvider using our CustomUserDetailsService and BCryptPasswordEncoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager bean for programmatically authenticating login requests.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt with work factor of 12 rounds (2^12 key derivations).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
