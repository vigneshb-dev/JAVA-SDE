package com.backendlearninglab.spring.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Request DTO for placing an order.
 * <p>
 * TEACHING FOCUS: Nested Validation via @Valid
 * Placing @Valid on the 'items' list instructs Spring's Validator to iterate
 * through each item in the collection and execute its individual validation constraints!
 */
public record CreateOrderRequest(
        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be positive")
        Long userId,

        @NotBlank(message = "Shipping address cannot be blank")
        String shippingAddress,

        @NotEmpty(message = "Order must contain at least one line item")
        @Valid // CRITICAL: Triggers validation on each element of the list!
        List<OrderItemRequestDto> items
) {
    public record OrderItemRequestDto(
            @NotNull(message = "Product ID is required")
            @Positive(message = "Product ID must be positive")
            Long productId,

            @Positive(message = "Quantity must be at least 1")
            int quantity
    ) {}
}
