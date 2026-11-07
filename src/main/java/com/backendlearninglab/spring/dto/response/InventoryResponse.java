package com.backendlearninglab.spring.dto.response;

import com.backendlearninglab.spring.entity.InventoryEntity;

import java.time.Instant;

public record InventoryResponse(
        Long productId,
        String productName,
        String productSku,
        int availableStock,
        int reservedStock,
        int totalStock,
        String warehouseLocation,
        Instant lastRestockedAt
) {
    public static InventoryResponse fromEntity(InventoryEntity entity) {
        String name = entity.getProduct() != null ? entity.getProduct().getName() : "Unknown";
        String sku = entity.getProduct() != null ? entity.getProduct().getSku() : "UNKNOWN";
        return new InventoryResponse(
                entity.getProductId(),
                name,
                sku,
                entity.getAvailableStock(),
                entity.getReservedStock(),
                entity.getAvailableStock() + entity.getReservedStock(),
                entity.getWarehouseLocation(),
                entity.getLastRestockedAt()
        );
    }
}
