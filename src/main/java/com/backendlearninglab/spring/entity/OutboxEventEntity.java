package com.backendlearninglab.spring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entity representing an uncommitted or pending asynchronous event in the Transactional Outbox.
 * <p>
 * TEACHING FOCUS: The Dual-Write Problem & Transactional Outbox Pattern:
 * <ul>
 *   <li>When an order is created, the system must:
 *       1. Insert into PostgreSQL {@code orders} table.
 *       2. Publish {@code OrderCreatedEvent} to Apache Kafka.</li>
 *   <li><b>THE FLAW:</b> If the DB commit succeeds, but the Kafka network call drops or crashes,
 *       the database state says the order exists, but downstream services (inventory, notifications)
 *       NEVER hear about it! Data corruption!</li>
 *   <li><b>THE SOLUTION:</b> Write this {@link OutboxEventEntity} into PostgreSQL within the
 *       <b>EXACT SAME local database transaction</b> as the Order. A separate publisher reads
 *       pending outbox records and dispatches them to Kafka with retry safety.</li>
 * </ul>
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    public OutboxEventEntity() {}

    public OutboxEventEntity(String aggregateType, String aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
        this.processedAt = Instant.now();
    }
}
