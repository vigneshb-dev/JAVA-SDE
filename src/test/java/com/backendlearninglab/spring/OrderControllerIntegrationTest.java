package com.backendlearninglab.spring;

import com.backendlearninglab.model.UserRole;
import com.backendlearninglab.spring.dto.request.CreateOrderRequest;
import com.backendlearninglab.spring.dto.request.CreateProductRequest;
import com.backendlearninglab.spring.dto.request.CreateUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC & REST API End-to-End Integration Tests.
 * Uses MockMvc to exercise the full HTTP DispatcherServlet request pipeline.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "admin@example.com", roles = {"ADMIN", "USER", "INVENTORY_MANAGER"})
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/users - Creates user and returns HTTP 201")
    void testCreateUserSuccess() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "sarah.connor@example.com",
                "SecretPass123!",
                "Sarah Connor",
                UserRole.CUSTOMER
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("sarah.connor@example.com")))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist()); // Verifies password hash is NOT leaked
    }

    @Test
    @DisplayName("POST /api/v1/users - Validation failure returns HTTP 400 with field errors")
    void testCreateUserValidationFailure() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "not-an-email", // Invalid email
                "short",        // Too short (< 8 chars)
                "",             // Blank full name
                null
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.fullName", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/orders - End-to-end checkout with atomic stock deduction")
    void testOrderPlacementFlow() throws Exception {
        // 1. Create User
        CreateUserRequest userReq = new CreateUserRequest("buyer.john@example.com", "Password123!", "John Buyer", UserRole.CUSTOMER);
        String userJson = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(userJson).path("data").path("id").asLong();

        // 2. Create Product with initial stock of 10
        CreateProductRequest prodReq = new CreateProductRequest("DESK-STAND-01", "Standing Desk", "Electric",
                new BigDecimal("350.00"), "FURNITURE", 10);
        String prodJson = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(prodJson).path("data").path("id").asLong();

        // 3. Place Order for 2 desks
        CreateOrderRequest orderReq = new CreateOrderRequest(
                userId,
                "500 Tech Blvd, Silicon Valley, CA",
                List.of(new CreateOrderRequest.OrderItemRequestDto(productId, 2))
        );

        String orderResponseJson = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount", is(700.00)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(orderResponseJson).path("data").path("id").asLong();

        // 4. Verify Inventory was decremented to 8 available
        mockMvc.perform(get("/api/v1/inventory/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableStock", is(8)))
                .andExpect(jsonPath("$.data.reservedStock", is(2)));

        // 5. Query order by ID
        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerName", is("John Buyer")));
    }
}
