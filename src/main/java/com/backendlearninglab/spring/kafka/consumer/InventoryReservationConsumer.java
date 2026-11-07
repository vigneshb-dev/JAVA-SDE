package com.backendlearninglab.spring.kafka.consumer;

import com.backendlearninglab.spring.entity.ProcessedEventEntity;
import com.backendlearninglab.spring.kafka.config.KafkaTopicConfiguration;
import com.backendlearninglab.spring.kafka.event.OrderCreatedEvent;
import com.backendlearninglab.spring.repository.SpringDataProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer representing the Warehouse & Inventory Service.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li><b>Consumer Groups:</b> Uses groupId {@code "orderflow-inventory-service"}. Kafka allocates
 *       topic partitions across active instances of this group.</li>
 *   <li><b>Idempotent Consumer Pattern:</b> Defends against duplicate delivery caused by network retries
 *       or consumer rebalances. If {@code eventId} was already processed, it logs and skips!</li>
 *   <li><b>Poison Pill Simulation:</b> If an order number contains {@code "FAIL-INVENTORY"}, throws a
 *       runtime exception to demonstrate {@link org.springframework.kafka.listener.DefaultErrorHandler}
 *       retries and eventual routing to the Dead Letter Topic!</li>
 * </ul>
 */
@Component
public class InventoryReservationConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationConsumer.class);
    private static final String CONSUMER_NAME = "inventory-service";

    private final SpringDataProcessedEventRepository processedEventRepository;

    public InventoryReservationConsumer(SpringDataProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfiguration.ORDER_CREATED_TOPIC,
            groupId = "orderflow-inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[InventoryConsumer] Received OrderCreatedEvent [ID: {}] from Partition: {} Offset: {}",
                event.eventId(), partition, offset);

        // 1. IDEMPOTENCY CHECK: Have we already processed this event?
        if (processedEventRepository.existsByIdEventIdAndIdConsumerName(event.eventId(), CONSUMER_NAME)) {
            log.warn("[InventoryConsumer] DUPLICATE EVENT DETECTED! Event ID [{}] already processed. Skipping.",
                    event.eventId());
            return;
        }

        // 2. DELIBERATE POISON PILL TEST: Trigger failure to test Retry + DLT
        if (event.orderNumber() != null && event.orderNumber().contains("FAIL-INVENTORY")) {
            log.error("[InventoryConsumer] Poison pill triggered for Order #{}! Simulating processing crash.",
                    event.orderNumber());
            throw new RuntimeException("Simulated Inventory Processing Error for Order: " + event.orderNumber());
        }

        // 3. Process Business Logic: Reserve warehouse inventory
        for (OrderCreatedEvent.OrderItemPayload item : event.items()) {
            log.info("[InventoryConsumer] Reserving {} units of Product ID {} for Order #{}",
                    item.quantity(), item.productId(), event.orderNumber());
        }

        // 4. Record event processing to guarantee idempotency on future retries
        processedEventRepository.save(new ProcessedEventEntity(event.eventId(), CONSUMER_NAME));
        log.info("[InventoryConsumer] Successfully processed & committed OrderCreatedEvent [{}]", event.eventId());
    }
}
