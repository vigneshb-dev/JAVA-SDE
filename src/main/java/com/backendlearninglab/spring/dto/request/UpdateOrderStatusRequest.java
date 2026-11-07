package com.backendlearninglab.spring.dto.request;

import com.backendlearninglab.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Order status is required")
        OrderStatus status
) {}
