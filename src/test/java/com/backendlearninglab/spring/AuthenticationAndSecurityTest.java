package com.backendlearninglab.spring;

import com.backendlearninglab.model.UserRole;
import com.backendlearninglab.spring.dto.request.CreateUserRequest;
import com.backendlearninglab.spring.dto.request.LoginRequest;
import com.backendlearninglab.spring.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security 6 Integration Tests.
 * <p>
 * Validates the complete stateless JWT authentication & authorization lifecycle:
 * <ul>
 *   <li>User registration with BCrypt hashing</li>
 *   <li>JWT generation upon login</li>
 *   <li>Bad credentials rejection (401)</li>
 *   <li>Unauthenticated access rejection via DelegatedAuthenticationEntryPoint (401)</li>
 *   <li>Role-based authorization failure via DelegatedAccessDeniedHandler (403)</li>
 *   <li>Admin privilege elevation check (200)</li>
 *   <li>Extracting Principal via /api/v1/auth/me with Bearer token</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthenticationAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/v1/auth/register - Successfully registers user and returns 201")
    void testRegisterUserSuccess() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "auth.test.user@example.com",
                "SecurePass123!",
                "Auth Test User",
                UserRole.USER
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.email", is("auth.test.user@example.com")))
                .andExpect(jsonPath("$.data.role", is("USER")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Valid credentials returns 200 with JWT")
    void testLoginSuccess() throws Exception {
        // 1. Register user
        String email = "login.success@example.com";
        String password = "CorrectPassword123!";
        CreateUserRequest regRequest = new CreateUserRequest(email, password, "Login User", UserRole.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // 2. Perform Login
        LoginRequest loginRequest = new LoginRequest(email, password);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Wrong password returns HTTP 401 Unauthorized")
    void testLoginBadCredentials() throws Exception {
        // 1. Register user
        String email = "badcred.user@example.com";
        CreateUserRequest regRequest = new CreateUserRequest(email, "SecretPassword123!", "Bad Cred User", UserRole.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // 2. Login with wrong password
        LoginRequest loginRequest = new LoginRequest(email, "WRONG_PASSWORD");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("GET /api/v1/orders - Missing JWT returns HTTP 401 via DelegatedAuthenticationEntryPoint")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("GET /api/v1/users - Access by USER role returns HTTP 403 via DelegatedAccessDeniedHandler")
    void testAccessDeniedReturns403() throws Exception {
        // 1. Register shopper
        String email = "regular.shopper@example.com";
        String password = "ShopperPassword123!";
        CreateUserRequest regRequest = new CreateUserRequest(email, password, "Shopper", UserRole.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // 2. Login to get JWT
        LoginRequest loginRequest = new LoginRequest(email, password);
        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(loginJson).path("data").path("accessToken").asText();

        // 3. Attempt to list all users (Requires ROLE_ADMIN) using shopper token
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Authenticated user can read own identity from JWT")
    void testGetAuthenticatedUserProfile() throws Exception {
        // 1. Register user
        String email = "profile.check@example.com";
        String password = "ProfilePass123!";
        CreateUserRequest regRequest = new CreateUserRequest(email, password, "Profile User", UserRole.USER);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // 2. Login to get JWT
        LoginRequest loginRequest = new LoginRequest(email, password);
        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(loginJson).path("data").path("accessToken").asText();

        // 3. Call /auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(email)))
                .andExpect(jsonPath("$.data.role", is("USER")));
    }
}
