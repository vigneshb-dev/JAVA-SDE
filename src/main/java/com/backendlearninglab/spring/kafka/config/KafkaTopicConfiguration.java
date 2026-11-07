package com.backendlearninglab.spring.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import org.springframework.context.annotation.Profile;

/**
 * Topic definitions for OrderFlow's Event-Driven Architecture.
 * <p>
 * TEACHING FOCUS: Partitions and Scaling:
 * <ul>
 *   <li><b>Partitions:</b> The fundamental unit of parallelism in Kafka. A topic with 3 partitions
 *       allows up to 3 consumers in the same consumer group to read in parallel.</li>
 *   <li><b>Replication Factor:</b> In production clusters (e.g. 3 brokers), a replication factor of 3
 *       ensures 2 brokers can fail without data loss. In local development, replication factor is 1.</li>
 *   <li><b>Dead Letter Topic (DLT):</b> Dedicated topic for storing unprocessable "poison pills"
 *       without stalling the main partition pipeline.</li>
 * </ul>
 */
@Configuration
@Profile("!dev")
public class KafkaTopicConfiguration {

    public static final String ORDER_CREATED_TOPIC = "order-created-events";
    public static final String ORDER_CREATED_DLT = "order-created-events-dlt";
    public static final String INVENTORY_EVENTS_TOPIC = "inventory-events";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedDlt() {
        return TopicBuilder.name(ORDER_CREATED_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(INVENTORY_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
