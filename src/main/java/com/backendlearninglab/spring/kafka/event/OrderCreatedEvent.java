package com.backendlearninglab.spring.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain Event representing the creation of an order in the OrderFlow system.
 * <p>
 * Published to Apache Kafka topic {@code order-created-events}.
 * Consumed by:
 * 1. {@code InventoryReservationConsumer} (updates warehouse stock)
 * 2. {@code CustomerNotificationConsumer} (simulates sending confirmation email)
 */
public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        String orderNumber,
        Long userId,
        String customerEmail,
        BigDecimal totalAmount,
        List<OrderItemPayload> items,
        Instant occurredAt
) {
    public record OrderItemPayload(
            Long productId,
            int quantity,
            BigDecimal unitPrice
    ) {}

    public static OrderCreatedEvent of(Long orderId, String orderNumber, Long userId,
                                      String customerEmail, BigDecimal totalAmount,
                                      List<OrderItemPayload> items) {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                orderId,
                orderNumber,
                userId,
                customerEmail,
                totalAmount,
                items,
                Instant.now()
        );
    }
}
