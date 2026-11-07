package com.backendlearninglab.spring;

import com.backendlearninglab.model.UserRole;
import com.backendlearninglab.spring.dto.request.CreateOrderRequest;
import com.backendlearninglab.spring.dto.request.CreateProductRequest;
import com.backendlearninglab.spring.dto.request.CreateUserRequest;
import com.backendlearninglab.spring.dto.response.OrderResponse;
import com.backendlearninglab.spring.dto.response.ProductResponse;
import com.backendlearninglab.spring.dto.response.UserResponse;
import com.backendlearninglab.spring.entity.OutboxEventEntity;
import com.backendlearninglab.spring.kafka.consumer.CustomerNotificationConsumer;
import com.backendlearninglab.spring.kafka.consumer.InventoryReservationConsumer;
import com.backendlearninglab.spring.kafka.event.OrderCreatedEvent;
import com.backendlearninglab.spring.kafka.outbox.OutboxPublisherService;
import com.backendlearninglab.spring.repository.SpringDataOutboxRepository;
import com.backendlearninglab.spring.repository.SpringDataProcessedEventRepository;
import com.backendlearninglab.spring.service.OrderJpaService;
import com.backendlearninglab.spring.service.ProductService;
import com.backendlearninglab.spring.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Event-Driven Architecture, Transactional Outbox & Observability Integration Tests.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li>1. Verifies that placing an order automatically persists an {@link OutboxEventEntity}
 *          in the exact same database transaction (resolving the Dual-Write problem).</li>
 *   <li>2. Verifies the Idempotent Consumer pattern prevents duplicate event execution.</li>
 *   <li>3. Verifies multiple independent consumer groups receive copies of the same event.</li>
 *   <li>4. Verifies poison pills trigger error handling.</li>
 *   <li>5. Verifies Distributed Tracing injecting {@code X-Correlation-ID} and Health check probes.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class KafkaEventProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderJpaService orderJpaService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private SpringDataProcessedEventRepository processedEventRepository;

    @Autowired
    private InventoryReservationConsumer inventoryConsumer;

    @Autowired
    private CustomerNotificationConsumer notificationConsumer;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Test
    @DisplayName("Transactional Outbox: Order placement atomically saves OutboxEventEntity in PostgreSQL")
    void testTransactionalOutboxPersistence() {
        // 1. Create User
        String email = "outbox.shopper." + System.currentTimeMillis() + "@example.com";
        UserResponse user = userService.createUser(new CreateUserRequest(email, "Password123!", "Outbox Buyer", UserRole.USER));

        // 2. Create Product with stock 20
        String sku = "OUTBOX-PROD-" + System.currentTimeMillis();
        ProductResponse product = productService.createProduct(new CreateProductRequest(
                sku, "Wireless Mouse", "Ergonomic Optical Mouse", new BigDecimal("49.99"), "ELECTRONICS", 20
        ));

        // 3. Place Order
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                user.id(),
                "456 Outbox Lane, Tech City",
                List.of(new CreateOrderRequest.OrderItemRequestDto(product.id(), 2))
        );
        OrderResponse placedOrder = orderJpaService.placeOrder(orderRequest);
        assertNotNull(placedOrder);

        // 4. Verify that an Outbox record exists with the matching aggregate ID!
        List<OutboxEventEntity> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventEntity.OutboxStatus.PENDING,
                PageRequest.of(0, 100)
        );
        assertFalse(pendingEvents.isEmpty(), "Outbox table must contain the pending OrderCreatedEvent!");

        boolean found = pendingEvents.stream()
                .anyMatch(e -> placedOrder.orderNumber().equals(e.getAggregateId()) && "OrderCreatedEvent".equals(e.getEventType()));
        assertTrue(found, "Outbox event for order " + placedOrder.orderNumber() + " must be saved in DB!");
    }

    @Test
    @DisplayName("Idempotent Consumer: Deduplication rejects identical duplicate event delivery")
    void testConsumerIdempotency() {
        String testEventId = "test-event-" + UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                testEventId,
                999L,
                "ORD-IDEMPOTENT-01",
                10L,
                "customer@example.com",
                new BigDecimal("99.99"),
                List.of(new OrderCreatedEvent.OrderItemPayload(1L, 1, new BigDecimal("99.99"))),
                java.time.Instant.now()
        );

        // 1. Initial Processing: Should process and record in processed_events table
        assertFalse(processedEventRepository.existsByIdEventIdAndIdConsumerName(testEventId, "inventory-service"));
        inventoryConsumer.onOrderCreated(event, 0, 100L);
        assertTrue(processedEventRepository.existsByIdEventIdAndIdConsumerName(testEventId, "inventory-service"),
                "Event must be marked as processed in DB");

        // 2. Duplicate Delivery: Calling onOrderCreated a 2nd time must be detected as duplicate and safely skipped!
        inventoryConsumer.onOrderCreated(event, 0, 100L);
        assertTrue(processedEventRepository.existsByIdEventIdAndIdConsumerName(testEventId, "inventory-service"),
                "Idempotency table still retains record; duplicate did not crash");
    }

    @Test
    @DisplayName("Pub-Sub: Both Inventory and Notification consumer groups process the same event")
    void testMultipleConsumerGroupsReceiveSameEvent() {
        String testEventId = "broadcast-event-" + UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                testEventId,
                888L,
                "ORD-BROADCAST-01",
                20L,
                "broadcaster@example.com",
                new BigDecimal("199.99"),
                List.of(new OrderCreatedEvent.OrderItemPayload(2L, 2, new BigDecimal("99.99"))),
                java.time.Instant.now()
        );

        // Both consumers process the event independently
        inventoryConsumer.onOrderCreated(event, 1, 201L);
        notificationConsumer.onOrderCreated(event, 1, 201L);

        // Both consumer groups must have their own separate idempotency records!
        assertTrue(processedEventRepository.existsByIdEventIdAndIdConsumerName(testEventId, "inventory-service"),
                "Inventory group must have processed the event");
        assertTrue(processedEventRepository.existsByIdEventIdAndIdConsumerName(testEventId, "notification-service"),
                "Notification group must have processed the event");
    }

    @Test
    @DisplayName("Failure Handling: Poison pill triggers simulated exception for retry & dead-letter pipeline")
    void testPoisonPillTriggersException() {
        OrderCreatedEvent poisonPill = new OrderCreatedEvent(
                "poison-" + UUID.randomUUID(),
                777L,
                "ORD-FAIL-INVENTORY-99", // Contains "FAIL-INVENTORY" trigger keyword
                30L,
                "poison@example.com",
                new BigDecimal("50.00"),
                List.of(),
                java.time.Instant.now()
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                inventoryConsumer.onOrderCreated(poisonPill, 2, 305L)
        );
        assertTrue(ex.getMessage().contains("Simulated Inventory Processing Error"));
    }

    @Test
    @DisplayName("Observability: GET /api/v1/health returns UP status, DB readiness, and JVM memory")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.data.database", is("UP")))
                .andExpect(jsonPath("$.data.jvmMemory.maxHeapMb", notNullValue()))
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    @DisplayName("Observability: Incoming requests are assigned a distributed tracing X-Correlation-ID")
    void testCorrelationIdTracing() throws Exception {
        String customCorrelationId = "custom-trace-uuid-" + UUID.randomUUID();

        // 1. Pass custom correlation ID -> Server echoes it back
        mockMvc.perform(get("/api/v1/health")
                        .header("X-Correlation-ID", customCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", customCorrelationId));

        // 2. Omit header -> Server generates and returns a new correlation ID
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"));
    }
}
