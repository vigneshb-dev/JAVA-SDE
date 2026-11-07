package com.backendlearninglab.spring.kafka.producer;

import com.backendlearninglab.spring.kafka.config.KafkaTopicConfiguration;
import com.backendlearninglab.spring.kafka.event.OrderCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer responsible for dispatching OrderFlow domain events to Apache Kafka.
 * <p>
 * TEACHING FOCUS: Partition Keys & Asynchronous Non-Blocking I/O:
 * <ul>
 *   <li><b>Partition Key:</b> Notice we use {@code String.valueOf(event.orderId())} as the record key.
 *       Kafka computes {@code murmur2(key) % partitionCount}. Every event belonging to the SAME order
 *       is guaranteed to land in the EXACT SAME partition, preserving strict chronological ordering!</li>
 *   <li><b>ProducerRecord vs KafkaTemplate.send():</b> {@code KafkaTemplate} places the record into
 *       the Producer's internal {@code RecordAccumulator} memory buffer. It does NOT block the HTTP request!</li>
 *   <li><b>CompletableFuture Callbacks:</b> We attach non-blocking callbacks via {@code .whenComplete(...)}
 *       to log broker acknowledgment (partition, offset) or handle delivery failures.</li>
 * </ul>
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an OrderCreatedEvent to Kafka topic asynchronously.
     *
     * @param event The immutable domain event payload
     * @return CompletableFuture holding the broker's acknowledgment metadata
     */
    public CompletableFuture<SendResult<String, Object>> publishOrderCreated(OrderCreatedEvent event) {
        String partitionKey = String.valueOf(event.orderId());
        String topic = KafkaTopicConfiguration.ORDER_CREATED_TOPIC;

        log.info("Sending OrderCreatedEvent [ID: {}] for Order #{} to topic '{}' with key '{}'",
                event.eventId(), event.orderNumber(), topic, partitionKey);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, partitionKey, event);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("FAILED to deliver OrderCreatedEvent [ID: {}] to Kafka topic '{}': {}",
                        event.eventId(), topic, ex.getMessage(), ex);
            } else {
                log.info("Successfully delivered OrderCreatedEvent [ID: {}] -> Topic: '{}' | Partition: {} | Offset: {} | Timestamp: {}",
                        event.eventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().timestamp());
            }
        });

        return future;
    }
}
