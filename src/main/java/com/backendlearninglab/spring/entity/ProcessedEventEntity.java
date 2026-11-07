package com.backendlearninglab.spring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity for tracking processed events to guarantee Idempotent Consumer behavior.
 * <p>
 * TEACHING FOCUS: The Myth of Automatic "Exactly-Once" Processing in Distributed Systems:
 * <ul>
 *   <li>Kafka guarantees <b>At-Least-Once Delivery</b> by default when using acknowledgement-based commits.</li>
 *   <li>If a consumer finishes processing an event (e.g. reserving inventory or sending email) but crashes
 *       BEFORE committing its offset back to Kafka, Kafka rebalances and redelivers the SAME event to another consumer!</li>
 *   <li>Without idempotency, inventory gets deducted twice, or customers receive 2 emails!</li>
 *   <li><b>THE IDEMPOTENCY KEY:</b> By inserting a record into {@code processed_events} with primary key
 *       {@code (event_id, consumer_name)}, any duplicate event delivery triggers a primary key conflict
 *       and is gracefully ignored as a duplicate!</li>
 * </ul>
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @Embeddable
    public static class ProcessedEventId implements Serializable {
        @Column(name = "event_id", length = 64)
        private String eventId;

        @Column(name = "consumer_name", length = 100)
        private String consumerName;

        public ProcessedEventId() {}

        public ProcessedEventId(String eventId, String consumerName) {
            this.eventId = eventId;
            this.consumerName = consumerName;
        }

        public String getEventId() {
            return eventId;
        }

        public String getConsumerName() {
            return consumerName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProcessedEventId that = (ProcessedEventId) o;
            return Objects.equals(eventId, that.eventId) && Objects.equals(consumerName, that.consumerName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, consumerName);
        }
    }

    @EmbeddedId
    private ProcessedEventId id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(String eventId, String consumerName) {
        this.id = new ProcessedEventId(eventId, consumerName);
        this.processedAt = Instant.now();
    }

    public ProcessedEventId getId() {
        return id;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
