package com.backendlearninglab.spring.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "SKU is required")
        @Pattern(regexp = "^[A-Z0-9_-]{3,64}$", message = "SKU must contain uppercase alphanumeric characters, dashes or underscores")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least $0.01")
        BigDecimal price,

        @NotBlank(message = "Category is required")
        String category,

        int initialStock
) {}
