# Apache Kafka & Event-Driven Architecture Demos

Runnable Kafka laboratories demonstrating distributed commit log fundamentals:

| Demo Class | Location | Concepts Covered |
| :--- | :--- | :--- |
| **Standalone Kafka Simulation** | `examples/kafka/StandaloneKafkaDemo.java` | Append-only partitioned commit logs, key-based partition hashing (`hash(key) % partitionCount`), consumer groups reading independently from offsets, and consumer rebalancing. |

### How to Run:
```bash
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.examples.kafka.StandaloneKafkaDemo"
```
