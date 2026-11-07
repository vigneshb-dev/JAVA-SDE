package com.backendlearninglab.spring.dto.response;

import com.backendlearninglab.model.OrderStatus;
import com.backendlearninglab.spring.entity.OrderEntity;
import com.backendlearninglab.spring.entity.OrderItemEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long userId,
        String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        String shippingAddress,
        Instant createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse fromEntity(OrderEntity entity) {
        String customer = (entity.getUser() != null) ? entity.getUser().getFullName() : "Unknown";
        List<OrderItemResponse> itemDtos = (entity.getItems() != null)
                ? entity.getItems().stream().map(OrderItemResponse::fromEntity).toList()
                : List.of();

        return new OrderResponse(
                entity.getId(),
                entity.getOrderNumber(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                customer,
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getShippingAddress(),
                entity.getCreatedAt(),
                itemDtos
        );
    }

    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            String productSku,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
        public static OrderItemResponse fromEntity(OrderItemEntity entity) {
            String name = (entity.getProduct() != null) ? entity.getProduct().getName() : "Unknown";
            String sku = (entity.getProduct() != null) ? entity.getProduct().getSku() : "UNKNOWN";
            return new OrderItemResponse(
                    entity.getId(),
                    entity.getProduct() != null ? entity.getProduct().getId() : null,
                    name,
                    sku,
                    entity.getQuantity(),
                    entity.getUnitPrice(),
                    entity.getSubtotal()
            );
        }
    }
}
