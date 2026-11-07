package com.backendlearninglab.spring;

import com.backendlearninglab.model.UserRole;
import com.backendlearninglab.spring.dto.request.CreateOrderRequest;
import com.backendlearninglab.spring.dto.request.CreateProductRequest;
import com.backendlearninglab.spring.dto.request.CreateUserRequest;
import com.backendlearninglab.spring.dto.response.InventoryResponse;
import com.backendlearninglab.spring.dto.response.ProductResponse;
import com.backendlearninglab.spring.dto.response.UserResponse;
import com.backendlearninglab.spring.service.InventoryService;
import com.backendlearninglab.spring.service.OrderJpaService;
import com.backendlearninglab.spring.service.ProductService;
import com.backendlearninglab.spring.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates Spring's @Transactional Rollback Guarantees.
 */
@SpringBootTest
@ActiveProfiles("dev")
class JpaTransactionRollbackTest {

    @Autowired
    private OrderJpaService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("Verify @Transactional rollback on unexpected exception - Stock and Order remain untouched")
    void testTransactionalRollbackOnFailure() {
        // 1. Setup user and product with initial stock 5
        UserResponse user = userService.createUser(new CreateUserRequest(
                "rollback.user@example.com", "Password123!", "Rollback Tester", UserRole.CUSTOMER));

        ProductResponse product = productService.createProduct(new CreateProductRequest(
                "SKU-ROLLBACK-01", "Rollback Item", "Test", new BigDecimal("50.00"), "MISC", 5));

        InventoryResponse initialInventory = inventoryService.getInventoryByProductId(product.id());
        assertEquals(5, initialInventory.availableStock());

        // 2. Attempt order with deliberate failure
        CreateOrderRequest request = new CreateOrderRequest(
                user.id(),
                "123 Test St",
                List.of(new CreateOrderRequest.OrderItemRequestDto(product.id(), 2))
        );

        assertThrows(IllegalStateException.class, () -> {
            orderService.placeOrderWithDeliberateFailure(request);
        });

        // 3. Verify stock was ROLLED BACK to 5 (not decremented to 3!)
        InventoryResponse postFailureInventory = inventoryService.getInventoryByProductId(product.id());
        assertEquals(5, postFailureInventory.availableStock(), "Available stock must remain untouched after rollback");
        assertEquals(0, postFailureInventory.reservedStock(), "Reserved stock must remain 0 after rollback");

        // 4. Verify no order was persisted in user's history
        assertTrue(orderService.getOrdersByUserId(user.id()).isEmpty(), "No order should exist after rollback");
    }
}
