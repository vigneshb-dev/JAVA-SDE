package com.backendlearninglab.spring.dto.response;

import com.backendlearninglab.spring.entity.ProductEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String category,
        boolean active,
        Instant createdAt
) {
    public static ProductResponse fromEntity(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getSku(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategory(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
