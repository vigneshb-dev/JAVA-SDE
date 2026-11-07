package com.backendlearninglab.spring.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Kafka Consumer & Resilient Error Handling Configuration.
 * <p>
 * TEACHING FOCUS: The Poison Pill Problem & Dead Letter Topics:
 * <ul>
 *   <li><b>POISON PILL:</b> A message with corrupt syntax or un-deserializable JSON arrives on the topic.
 *       Without {@link ErrorHandlingDeserializer}, the consumer crashes on poll(), restarts, polls the SAME
 *       poison pill, crashes again, and loops infinitely! Partition processing halts completely!</li>
 *   <li><b>DEAD LETTER TOPIC (DLT):</b> Our {@link DefaultErrorHandler} retries transient exceptions
 *       up to 2 times with a 1-second backoff. If processing still fails, {@link DeadLetterPublishingRecoverer}
 *       routes the failed record to {@code order-created-events-dlt} and commits the main offset!
 *       The partition continues moving forward without dropping data!</li>
 * </ul>
 */
@Configuration
public class KafkaConsumerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfiguration.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "orderflow-default-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Key Deserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);

        // Value Deserializer (Robust ErrorHandlingDeserializer wrapping Jackson)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.backendlearninglab.*");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Configure Dead Letter Publishing Recoverer to route failed records to .DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // Retry 2 times with 1000ms delay before routing to Dead Letter Topic
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));

        // Log warning upon each retry
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Kafka Consumer delivery attempt {} failed for topic {} partition {} offset {}: {}",
                    deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage());
        });

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
