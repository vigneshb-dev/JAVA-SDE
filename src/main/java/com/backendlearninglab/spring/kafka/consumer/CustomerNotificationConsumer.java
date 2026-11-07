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
 * Consumer representing the Email & Customer Notification Service.
 * <p>
 * TEACHING FOCUS: Publish-Subscribe Broadcast Across Different Consumer Groups:
 * <ul>
 *   <li>Notice the {@code groupId = "orderflow-notification-service"}!</li>
 *   <li>Because this group ID is different from {@code "orderflow-inventory-service"}, Kafka delivers
 *       a copy of <b>EVERY SINGLE EVENT</b> to BOTH groups!</li>
 *   <li>Each group maintains its own distinct committed offset in {@code __consumer_offsets}.
 *       If the notification service is down for maintenance, inventory processing continues without delay,
 *       and notifications catch up when restarted! Complete decoupling!</li>
 * </ul>
 */
@Component
public class CustomerNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(CustomerNotificationConsumer.class);
    private static final String CONSUMER_NAME = "notification-service";

    private final SpringDataProcessedEventRepository processedEventRepository;

    public CustomerNotificationConsumer(SpringDataProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfiguration.ORDER_CREATED_TOPIC,
            groupId = "orderflow-notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[NotificationConsumer] Received OrderCreatedEvent [ID: {}] from Partition: {} Offset: {}",
                event.eventId(), partition, offset);

        // Idempotency verification
        if (processedEventRepository.existsByIdEventIdAndIdConsumerName(event.eventId(), CONSUMER_NAME)) {
            log.warn("[NotificationConsumer] Duplicate event [{}] already processed for notifications. Skipping.",
                    event.eventId());
            return;
        }

        // Simulate asynchronous customer email generation
        log.info("[NotificationConsumer] Sending Order Confirmation Email to '{}' for Order #{} (Total: ${})",
                event.customerEmail(), event.orderNumber(), event.totalAmount());

        // Mark processed
        processedEventRepository.save(new ProcessedEventEntity(event.eventId(), CONSUMER_NAME));
        log.info("[NotificationConsumer] Email dispatched and event [{}] recorded.", event.eventId());
    }
}
