# Apache Kafka Fundamentals & CLI Guide

> **"A database is an index over a state; Kafka is the immutable log of everything that ever happened."**

---

## 1. Why Kafka Exists & The Core Problem It Solves

In early architectures, microservices communicated via point-to-point synchronous REST calls:

```
[ Checkout Service ] ──(HTTP POST)──► [ Inventory Service ]
         │
         ├──(HTTP POST)──► [ Notification Service ]
         │
         ├──(HTTP POST)──► [ Payment Service ]
         │
         └──(HTTP POST)──► [ Analytics / Data Lake ]
```

### The 4 Fatal Flaws of Synchronous Point-to-Point REST:
1. **Cascading Failures:** If the Analytics service is slow or down, the Checkout Service blocks and fails, rejecting paying customers!
2. **Tight Temporal Coupling:** The producer and consumer must be available at the exact same millisecond.
3. **High Latency:** Checkout response time equals $\text{Time}(\text{Checkout}) + \text{Time}(\text{Inventory}) + \text{Time}(\text{Email}) + \text{Time}(\text{Analytics})$.
4. **$N \times M$ Integration Sprawl:** Adding a new service requires modifying every existing producer to send another HTTP request.

### The Kafka Solution: The Immutable Distributed Commit Log

```
[ Checkout Service ] ──► (Publish Event) ──► [ Apache Kafka: order-events Topic ]
                                                        │
                      ┌─────────────────────────────────┼─────────────────────────────────┐
                      ▼                                 ▼                                 ▼
         [ Inventory Service ]             [ Notification Service ]          [ Analytics Service ]
         (Consumer Group A)                (Consumer Group B)                (Consumer Group C)
         Offset: 104                       Offset: 104                       Offset: 42
```

In Kafka:
- The Checkout Service writes **one immutable event** (`OrderCreatedEvent`) to a Kafka topic in ~2 ms and immediately returns HTTP 201 to the customer.
- Consumers read asynchronously at their own pace without impacting the producer or each other.
- If the Notification Service goes down for 2 hours, events safely queue in Kafka; when restarted, it resumes from its last committed offset with zero data loss!

---

## 2. Core Concepts & Terminology

| Concept | What It Is | Real-World Analogy |
| :--- | :--- | :--- |
| **Broker** | A single Kafka server node running in a cluster. Brokers receive messages from producers, write them to disk, and serve consumers. | A post office sorting warehouse. |
| **Topic** | A logical stream or category of messages (e.g., `order-created-events`). | A newspaper publication (e.g. *The Financial Times*). |
| **Partition** | An ordered, append-only commit log file on disk. A topic is split into 1 or more partitions across brokers for horizontal scalability. | Individual print printing presses running concurrently. |
| **Offset** | A strictly sequential 64-bit integer ($0, 1, 2, ...$) assigned to each message within a partition. It is the immutable position of the record. | Page numbers in a book. |
| **Producer** | A client application that publishes records to Kafka topics. | The author writing newspaper articles. |
| **Consumer** | A client application that subscribes to topics and processes records. | A subscriber reading newspaper articles. |
| **Consumer Group** | A collection of consumer instances cooperating to read from a topic. Each partition is consumed by **exactly one consumer** within the group. | A household sharing one newspaper subscription. |
| **Replication Factor**| Number of copies of each partition stored across distinct brokers (e.g., factor of 3 means 1 leader + 2 followers). | Backing up important contracts in 3 separate safes. |
| **Retention Period** | Time-based (`log.retention.hours=168` / 7 days) or size-based retention before Kafka deletes old log segments. | Archive retention policy in a library. |

---

## 3. Architectural Comparisons: Why Kafka Is Unique

### A. Kafka vs. Relational Database (PostgreSQL)
- **PostgreSQL:** Stores **current state** (e.g. `order.status = 'SHIPPED'`). Reading an item does not change it. Updates overwrite data.
- **Kafka:** Stores the **full history of state transitions** as an append-only commit log:
  1. `OrderCreated` ($t=0$)
  2. `PaymentAuthorized` ($t=1$)
  3. `InventoryReserved` ($t=2$)
  4. `OrderShipped` ($t=3$)
- **Key Takeaway:** You can rebuild any database state by replaying the Kafka commit log from offset 0!

### B. Kafka vs. Traditional Message Queues (RabbitMQ / ActiveMQ / SQS)
| Feature | Traditional Queue (RabbitMQ) | Apache Kafka |
| :--- | :--- | :--- |
| **Message Deletion** | Messages are **deleted immediately** after consumer acknowledgment (`ack`). | Messages are **retained on disk** for days/weeks. Reading never deletes! |
| **Multiple Consumers** | Competing consumers pop messages from the same queue (destructive read). | Different consumer groups can read the exact same message independently. |
| **Replayability** | Cannot rewind. Once read, message is gone. | Consumers can reset their offset to 0 and re-read historical messages anytime! |
| **Throughput** | ~20,000 to 50,000 msgs/sec (RAM/Erlang overhead). | **1,000,000+ msgs/sec** via Linux OS page cache, sequential disk I/O, and zero-copy (`sendfile`). |

### C. Kafka vs. Redis Pub/Sub
- **Redis Pub/Sub:** "Fire-and-forget". If a subscriber is offline for 100 ms, the message is permanently lost! No persistence.
- **Kafka:** Highly durable. Every message is written to sequential disk log segments and replicated across brokers before acknowledging the producer.

---

## 4. Kafka CLI Cheatsheet (Local Development & Debugging)

If running Kafka locally via Docker (`docker-compose.yml`), execute these commands inside the `orderflow-kafka` container:

```bash
# Open interactive shell in the Kafka container
docker exec -it orderflow-kafka bash
```

### 1. Create a Topic
```bash
kafka-topics --bootstrap-server localhost:9092 \
  --create \
  --topic order-created-events \
  --partitions 3 \
  --replication-factor 1
```
- `--partitions 3`: Splits the topic into 3 independent parallel commit logs.
- `--replication-factor 1`: 1 copy per record (use 3 in production).

### 2. List & Inspect Topics
```bash
# List all topics
kafka-topics --bootstrap-server localhost:9092 --list

# Describe a topic (shows partitions, leaders, replicas, and ISR)
kafka-topics --bootstrap-server localhost:9092 \
  --describe \
  --topic order-created-events
```
*Output explains:*
```
Topic: order-created-events   TopicId: ...  PartitionCount: 3   ReplicationFactor: 1
  Partition: 0  Leader: 1  Replicas: 1  Isr: 1
  Partition: 1  Leader: 1  Replicas: 1  Isr: 1
  Partition: 2  Leader: 1  Replicas: 1  Isr: 1
```
- **ISR (In-Sync Replicas):** Replicas that are fully caught up with the partition leader.

### 3. Produce Messages Interactively
```bash
kafka-console-producer --bootstrap-server localhost:9092 \
  --topic order-created-events \
  --property "parse.key=true" \
  --property "key.separator=:"
```
Type key-value messages (e.g., `ORD-101:{"orderId":101,"total":49.99}`).
- **Why Keys Matter:** All messages with the same key (`ORD-101`) are hashed to the **same partition**, guaranteeing in-order processing!

### 4. Consume Messages from Beginning
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-created-events \
  --from-beginning \
  --property "print.key=true" \
  --property "print.partition=true" \
  --property "print.offset=true"
```
Shows the exact partition, offset, key, and value payload!

### 5. Inspect Consumer Groups & Lag
```bash
# List all active consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Inspect consumer offsets and LAG for our inventory service
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe \
  --group orderflow-inventory-service
```
*Critical Output Table:*
```
TOPIC                PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
order-created-events 0          150             150             0
order-created-events 1          120             122             2
order-created-events 2          98              98              0
```
- **LOG-END-OFFSET:** The latest offset produced to the partition.
- **CURRENT-OFFSET:** The last offset processed and committed by the consumer group.
- **LAG:** Unprocessed messages ($\text{LOG-END-OFFSET} - \text{CURRENT-OFFSET}$). A growing lag indicates your consumer is too slow or downstream database locks are stalling processing!
