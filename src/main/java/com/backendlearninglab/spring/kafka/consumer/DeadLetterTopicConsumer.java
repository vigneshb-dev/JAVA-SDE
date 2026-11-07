package com.backendlearninglab.spring.kafka.consumer;

import com.backendlearninglab.spring.kafka.config.KafkaTopicConfiguration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer monitoring the Dead Letter Topic (DLT).
 * <p>
 * TEACHING FOCUS: Dead Letter Observability & Alerting:
 * <ul>
 *   <li>When a message exhausts all retries in {@link org.springframework.kafka.listener.DefaultErrorHandler},
 *       it is automatically rerouted here.</li>
 *   <li>The main consumer pipeline does NOT crash or stop.</li>
 *   <li>In production systems, this consumer alerts engineers via PagerDuty / Slack or stores
 *       unrecoverable events into an administrative inspection dashboard for manual replay.</li>
 * </ul>
 */
@Component
public class DeadLetterTopicConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterTopicConsumer.class);

    @KafkaListener(
            topics = KafkaTopicConfiguration.ORDER_CREATED_DLT,
            groupId = "orderflow-dlt-monitor-service"
    )
    public void onDeadLetterReceived(ConsumerRecord<String, Object> record) {
        log.error("================================================================================");
        log.error("CRITICAL ALERT: POISON PILL ROUTED TO DEAD LETTER TOPIC!");
        log.error("Topic: {} | Partition: {} | Offset: {}", record.topic(), record.partition(), record.offset());
        log.error("Record Key: {}", record.key());
        log.error("Payload: {}", record.value());
        log.error("Action Required: Inspect payload for malformed data or permanent schema drift.");
        log.error("================================================================================");
    }
}
