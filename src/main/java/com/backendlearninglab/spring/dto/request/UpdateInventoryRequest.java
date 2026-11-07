package com.backendlearninglab.spring.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryRequest(
        @NotNull(message = "Available stock is required")
        @Min(value = 0, message = "Available stock cannot be negative")
        Integer availableStock,

        String warehouseLocation
) {}
