# 10 Progressive Backend Engineering Challenges

> **"Skill is not acquired by watching someone else code. It is forged by wrestling with compiler errors, failing tests, and unexpected race conditions."**

Do NOT consult [docs/challenges-solutions.md](file:///g:/JAVA%20SDE/docs/challenges-solutions.md) until you have spent at least 45 minutes designing, implementing, and testing your own solution.

---

### Challenge 1: Keyset (Cursor) Pagination for Product Catalog
- **Requirements:** Add an endpoint `GET /api/v1/products/cursor?lastSeenId=100&limit=20` that returns the next page of active products without using SQL `OFFSET`.
- **Constraints:** Must execute in $O(1)$ constant time regardless of whether browsing page 1 or page 5,000.
- **Expected Behavior:** Returns JSON payload with `items` list, `nextCursor` ID, and `hasNextPage` boolean.
- **Hint:** Use `WHERE id > :lastSeenId ORDER BY id ASC LIMIT :limit` over a B-Tree index on `id`.

---

### Challenge 2: Multi-Field Product Search with Dynamic Filters
- **Requirements:** Add `GET /api/v1/products/search?name=desk&minPrice=100&maxPrice=500&category=FURNITURE`. Any parameter may be null or omitted.
- **Constraints:** Do NOT concatenate raw SQL strings (prevents SQL injection).
- **Expected Behavior:** Dynamically constructs the WHERE clause filtering only provided non-null criteria.
- **Hint:** Look into Spring Data JPA's `JpaSpecificationExecutor` and `CriteriaBuilder`.

---

### Challenge 3: Optimistic Locking with JPA `@Version`
- **Requirements:** Prevent the "Lost Update" problem where two admins concurrently edit product price, and the last write silently overwrites the first.
- **Constraints:** Must detect the conflict and throw an `OptimisticLockingFailureException` mapped to HTTP `409 Conflict`.
- **Expected Behavior:** Add a `@Version private Long version;` field to `ProductEntity`.
- **Hint:** Test using two threads attempting to commit price updates using the same initial version number.

---

### Challenge 4: Two-Phase Inventory Reservation with Expiration TTL
- **Requirements:** When a shopper adds merchandise to their cart, temporarily reserve stock for 15 minutes. If checkout is not completed within 15 minutes, automatically release the stock.
- **Constraints:** Use Redis with a 15-minute TTL to store the temporary reservation key.
- **Expected Behavior:** If the user checks out within 15 minutes, the reservation is confirmed in PostgreSQL. If the Redis key expires, inventory returns to the available pool.
- **Hint:** Use Redis `SET reservation:{userId}:{productId} {quantity} EX 900`.

---

### Challenge 5: Distributed Token Bucket Rate Limiting per User
- **Requirements:** Restrict authenticated users to 10 requests per minute on checkout (`POST /api/v1/orders`), returning `HTTP 429 Too Many Requests` when exceeded.
- **Constraints:** Must use an atomic Redis command or Lua script to prevent race conditions across horizontal replicas.
- **Expected Behavior:** Returns headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `Retry-After`.
- **Hint:** Use Redis `INCR` combined with `EXPIRE` or a Redis Lua script.

---

### Challenge 6: API Request Idempotency via Idempotency-Key Header
- **Requirements:** Ensure that if a mobile app retries `POST /api/v1/orders` due to a flaky Wi-Fi connection, the order is executed exactly once.
- **Constraints:** Require clients to supply header `Idempotency-Key: <UUID>`.
- **Expected Behavior:** If the key was already processed, return the cached HTTP response immediately without re-billing the customer or deducting stock again.
- **Hint:** Use Redis or PostgreSQL table `idempotency_keys` storing `(key, response_body, status, created_at)` with a 24-hour TTL.

---

### Challenge 7: Asynchronous Order Cancellation Saga
- **Requirements:** Support `POST /api/v1/orders/{id}/cancel`.
- **Constraints:** Asynchronously publish an `OrderCancelledEvent` to Kafka.
- **Expected Behavior:** An `InventoryRestorationConsumer` receives the event and restores the deducted stock in PostgreSQL.
- **Hint:** Follow the same event-driven pattern as `OrderCreatedEvent` using an Outbox record.

---

### Challenge 8: Consumer Exponential Backoff with DLQ Dead-Letter Alerting
- **Requirements:** Configure `@KafkaListener` to retry transient downstream exceptions with exponential backoff: 1s, 2s, 4s, 8s (up to 4 attempts).
- **Constraints:** If all retries fail, publish the failed record to `order-events-dlt` and send an alert log.
- **Expected Behavior:** Healthy messages on other partitions are not delayed while retrying.
- **Hint:** Configure Spring Kafka's `ExponentialBackOffWithMaxRetries` in `DefaultErrorHandler`.

---

### Challenge 9: Asynchronous Audit Logging Pipeline
- **Requirements:** Track every administrative mutation (`POST`, `PUT`, `DELETE` on products or user status) into an immutable `audit_logs` table.
- **Constraints:** The audit write must NOT add latency to the user's HTTP request.
- **Expected Behavior:** Use Spring's `@Async` with a custom thread pool or an event-driven Kafka `audit-events` topic.
- **Hint:** Use Spring Application Events (`ApplicationEventPublisher`) with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

---

### Challenge 10: The Ultimate Challenge — Implement a New Feature Solo
- **Requirements:** Design, implement, and test a **Customer Review & Rating System**:
  1. Customers can rate a purchased product (1 to 5 stars) with a text review.
  2. Products store an aggregate `average_rating` and `review_count`.
  3. Updating a review asynchronously recomputes the aggregate rating via Kafka or a scheduled job.
  4. Cache the top 10 reviews in Redis.
- **Rule:** Implement this entire feature **without asking AI to write a single class or method**! Use AI strictly to review your finished code and critique your SQL queries.
