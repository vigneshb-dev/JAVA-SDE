package com.backendlearninglab.examples.kafka;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EDUCATIONAL DEMONSTRATION: Apache Kafka Architecture Simulation in Pure Java.
 * <p>
 * TEACHING FOCUS: Demystifying Kafka Internals:
 * <ul>
 *   <li>1. <b>The Append-Only Commit Log:</b> Why Kafka is fundamentally different from a relational database or RabbitMQ.
 *       Messages are NOT deleted when read; they are appended to an immutable sequential log on disk.</li>
 *   <li>2. <b>Partitions & Keys:</b> {@code hash(key) % partitionCount} guarantees per-key FIFO ordering.</li>
 *   <li>3. <b>Consumer Groups & Offsets:</b> Each consumer group maintains its own independent offset pointer
 *       in {@code __consumer_offsets}. Reading does not destroy the message!</li>
 *   <li>4. <b>Consumer Rebalance:</b> Dynamic redistribution of partitions when consumer instances join or leave.</li>
 * </ul>
 */
public class StandaloneKafkaDemo {

    public record LogRecord(long offset, String key, String value, Instant timestamp) {}

    public static class SimulatedPartition {
        private final int partitionId;
        private final List<LogRecord> log = new ArrayList<>();
        private long nextOffset = 0;

        public SimulatedPartition(int partitionId) {
            this.partitionId = partitionId;
        }

        public synchronized LogRecord append(String key, String value) {
            LogRecord record = new LogRecord(nextOffset++, key, value, Instant.now());
            log.add(record);
            return record;
        }

        public synchronized List<LogRecord> readFromOffset(long fromOffset, int maxRecords) {
            List<LogRecord> results = new ArrayList<>();
            for (LogRecord r : log) {
                if (r.offset() >= fromOffset) {
                    results.add(r);
                    if (results.size() >= maxRecords) break;
                }
            }
            return results;
        }

        public int getPartitionId() {
            return partitionId;
        }

        public int size() {
            return log.size();
        }
    }

    public static class SimulatedTopic {
        private final String name;
        private final List<SimulatedPartition> partitions;

        public SimulatedTopic(String name, int partitionCount) {
            this.name = name;
            this.partitions = new ArrayList<>();
            for (int i = 0; i < partitionCount; i++) {
                partitions.add(new SimulatedPartition(i));
            }
        }

        public LogRecord produce(String key, String value) {
            // Kafka's Default Partitioner: hash(key) % numPartitions
            int partitionIndex = (key == null) ? 0 : Math.abs(key.hashCode()) % partitions.size();
            SimulatedPartition partition = partitions.get(partitionIndex);
            return partition.append(key, value);
        }

        public SimulatedPartition getPartition(int index) {
            return partitions.get(index);
        }

        public int getPartitionCount() {
            return partitions.size();
        }

        public String getName() {
            return name;
        }
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("      APACHE KAFKA ARCHITECTURE & INTERNALS SIMULATION (PURE JAVA)             ");
        System.out.println("================================================================================\n");

        SimulatedTopic topic = new SimulatedTopic("order-events", 3);
        System.out.println("[Step 1] Created Topic '" + topic.getName() + "' with 3 Partitions.\n");

        // 1. Producing Messages: Demonstrating Key-based Partitioning
        System.out.println("--- 1. PRODUCER PARTITIONING (Guaranteed FIFO by Key) ---");
        String[] orders = {"ORD-101", "ORD-102", "ORD-103", "ORD-101", "ORD-102"};
        String[] events = {"OrderCreated", "OrderCreated", "OrderCreated", "OrderPaid", "OrderCancelled"};

        for (int i = 0; i < orders.length; i++) {
            String key = orders[i];
            String payload = events[i];
            LogRecord record = topic.produce(key, payload);
            int partitionIndex = Math.abs(key.hashCode()) % topic.getPartitionCount();
            System.out.printf("  Produced: Key=%-8s Event=%-14s -> Partition %d | Offset %d%n",
                    key, payload, partitionIndex, record.offset());
        }

        System.out.println("\nNotice: Both ORD-101 events landed in the EXACT SAME partition in sequential offset order!");
        System.out.println("This is why Kafka guarantees strict ordering per key, even across thousands of concurrent orders!\n");

        // 2. Consumer Groups: Scaling and Independent Offset Tracking
        System.out.println("--- 2. CONSUMER GROUPS & OFFSET TRACKING ---");
        System.out.println("Scenario: Two independent microservices consume the SAME topic:");
        System.out.println("  Group 1: 'inventory-service' (has 2 consumers A and B)");
        System.out.println("  Group 2: 'notification-service' (has 1 consumer C)\n");

        // Group 1 Offsets: Map<ConsumerGroup, Map<PartitionId, Long>>
        Map<String, Map<Integer, Long>> committedOffsets = new ConcurrentHashMap<>();
        committedOffsets.put("inventory-service", new HashMap<>(Map.of(0, 0L, 1, 0L, 2, 0L)));
        committedOffsets.put("notification-service", new HashMap<>(Map.of(0, 0L, 1, 0L, 2, 0L)));

        // Consumer A reads Partitions 0 and 1
        System.out.println("[inventory-service: Consumer A] Polls Partitions 0 & 1:");
        pollAndPrint("inventory-service", "Consumer A", topic, List.of(0, 1), committedOffsets);

        // Consumer B reads Partition 2
        System.out.println("\n[inventory-service: Consumer B] Polls Partition 2:");
        pollAndPrint("inventory-service", "Consumer B", topic, List.of(2), committedOffsets);

        // Group 2: Notification Service reads ALL partitions from Offset 0!
        System.out.println("\n[notification-service: Consumer C] Polls All Partitions (Independent Offsets):");
        pollAndPrint("notification-service", "Consumer C", topic, List.of(0, 1, 2), committedOffsets);

        // 3. Consumer Rebalance Simulation
        System.out.println("\n--- 3. CONSUMER GROUP REBALANCE ---");
        System.out.println("Scenario: A new instance 'Consumer D' joins 'inventory-service'!");
        System.out.println("Kafka Rebalance Triggered: Partitions are reassigned 1-to-1:");
        System.out.println("  Consumer A -> Partition 0");
        System.out.println("  Consumer B -> Partition 1");
        System.out.println("  Consumer D -> Partition 2");
        System.out.println("Each consumer resumes reading exactly from where the previous owner committed its offset!\n");

        System.out.println("================================================================================");
        System.out.println("SIMULATION COMPLETE: Demonstrates append-only logs, partition keys, offsets, and pub-sub.");
        System.out.println("================================================================================");
    }

    private static void pollAndPrint(String groupId, String consumerName, SimulatedTopic topic,
                                     List<Integer> assignedPartitions,
                                     Map<String, Map<Integer, Long>> offsetsMap) {
        Map<Integer, Long> groupOffsets = offsetsMap.get(groupId);
        for (int pId : assignedPartitions) {
            long currentOffset = groupOffsets.getOrDefault(pId, 0L);
            List<LogRecord> records = topic.getPartition(pId).readFromOffset(currentOffset, 10);
            for (LogRecord r : records) {
                System.out.printf("    [%s] Read Partition %d | Offset %d | Key: %-7s | Value: %s%n",
                        consumerName, pId, r.offset(), r.key(), r.value());
                groupOffsets.put(pId, r.offset() + 1); // Commit offset
            }
        }
    }
}
