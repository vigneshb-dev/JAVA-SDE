# Redis In-Memory Caching & Data Structures Demos

Runnable Redis laboratories demonstrating in-memory mechanics and distributed patterns:

| Demo Class | Location | Concepts Covered |
| :--- | :--- | :--- |
| **Standalone Redis & LRU Simulation** | `examples/redis/StandaloneRedisDemo.java` | In-memory key-value storage, `SET`, `GET`, `DEL`, `EXPIRE`, TTL countdown to automatic eviction, and the Least Recently Used (LRU) eviction algorithm without requiring an external Redis daemon. |

### How to Run:
```bash
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.examples.redis.StandaloneRedisDemo"
```
