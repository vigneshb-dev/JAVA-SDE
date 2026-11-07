package com.backendlearninglab.spring.kafka.outbox;

import com.backendlearninglab.spring.entity.OutboxEventEntity;
import com.backendlearninglab.spring.kafka.event.OrderCreatedEvent;
import com.backendlearninglab.spring.kafka.producer.OrderEventProducer;
import com.backendlearninglab.spring.repository.SpringDataOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that reliably drains the Transactional Outbox table and publishes events to Apache Kafka.
 * <p>
 * TEACHING FOCUS: Resolving the Dual-Write Dilemma:
 * <ul>
 *   <li>The order checkout process writes an {@link OutboxEventEntity} in the SAME database transaction as the order.</li>
 *   <li>This publisher polls for {@code PENDING} outbox events every 2 seconds.</li>
 *   <li>Even if the server restarts or Kafka crashes, un-published events remain safely persisted in PostgreSQL!</li>
 *   <li>Once Kafka returns an acknowledgment (ACK), this service marks the outbox record as {@code PUBLISHED}.</li>
 * </ul>
 */
@Service
@EnableScheduling
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final SpringDataOutboxRepository outboxRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    public OutboxPublisherService(SpringDataOutboxRepository outboxRepository,
                                  OrderEventProducer orderEventProducer,
                                  ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.orderEventProducer = orderEventProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Scheduled poller that reads pending outbox events in chronological order and dispatches them.
     */
    @Scheduled(fixedDelayString = "${orderflow.outbox.poll-interval-ms:2000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEventEntity> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventEntity.OutboxStatus.PENDING,
                PageRequest.of(0, 50)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending events in Transactional Outbox. Publishing to Kafka...", pendingEvents.size());

        for (OutboxEventEntity outboxEvent : pendingEvents) {
            publishSingleEvent(outboxEvent);
        }
    }

    /**
     * Publishes a single outbox event to Kafka with retry tracking.
     */
    @Transactional
    public void publishSingleEvent(OutboxEventEntity outboxEvent) {
        try {
            if ("OrderCreatedEvent".equals(outboxEvent.getEventType())) {
                OrderCreatedEvent event = objectMapper.readValue(outboxEvent.getPayload(), OrderCreatedEvent.class);

                orderEventProducer.publishOrderCreated(event).whenComplete((res, ex) -> {
                    if (ex == null) {
                        outboxEvent.markPublished();
                        outboxRepository.save(outboxEvent);
                        log.info("Outbox event [ID: {}] marked PUBLISHED.", outboxEvent.getId());
                    } else {
                        handlePublishFailure(outboxEvent, ex);
                    }
                });
            } else {
                log.warn("Unknown outbox event type: {}", outboxEvent.getEventType());
                outboxEvent.markFailed();
                outboxRepository.save(outboxEvent);
            }
        } catch (Exception e) {
            handlePublishFailure(outboxEvent, e);
        }
    }

    private void handlePublishFailure(OutboxEventEntity outboxEvent, Throwable ex) {
        outboxEvent.incrementRetryCount();
        log.warn("Failed to publish outbox event [ID: {}] (Attempt {}): {}",
                outboxEvent.getId(), outboxEvent.getRetryCount(), ex.getMessage());

        if (outboxEvent.getRetryCount() >= 5) {
            log.error("Outbox event [ID: {}] exceeded max retries. Marking FAILED.", outboxEvent.getId());
            outboxEvent.markFailed();
        }
        outboxRepository.save(outboxEvent);
    }
}
