package com.backendlearninglab.spring.kafka.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event emitted when inventory has been confirmed and reserved for an order.
 */
public record InventoryReservedEvent(
        String eventId,
        Long orderId,
        Long productId,
        int quantityReserved,
        Instant reservedAt
) {
    public static InventoryReservedEvent of(Long orderId, Long productId, int quantityReserved) {
        return new InventoryReservedEvent(
                UUID.randomUUID().toString(),
                orderId,
                productId,
                quantityReserved,
                Instant.now()
        );
    }
}
