# PostgreSQL & Pure JDBC Engineering Guide

> **"Spring Boot hides JDBC behind `@Repository`, `JdbcTemplate`, and `@Transactional`. When performance degrades in production, you cannot fix what you do not understand."**

---

## 1. Relational Database Design & Normalization

### Normalization (3NF in OrderFlow):
1. **First Normal Form (1NF):** Each column holds atomic, indivisible values. No CSV strings like `tags="tech,gadget,mouse"`.
2. **Second Normal Form (2NF):** Satisfies 1NF and all non-key attributes are fully functionally dependent on the primary key. Line items live in `order_items`, not packed into columns on `orders`.
3. **Third Normal Form (3NF):** Satisfies 2NF and has no transitive dependencies. 
   - *Crucial Exception for Business Integrity:* Notice `order_items` explicitly stores `unit_price` at the moment of checkout, even though `products.price` exists. Why? Because catalog prices change! If a product price increases next month, past historical invoices must NEVER mutate!

### Database Constraints as the Last Line of Defense:
- **`PRIMARY KEY`**: Enforces uniqueness and creates a clustered/unique B-Tree index.
- **`FOREIGN KEY`**: Enforces referential integrity. Setting `ON DELETE RESTRICT` on users prevents accidental customer deletions while open orders exist.
- **`CHECK` Constraints**: Enforces domain invariants (`price >= 0.01`, `quantity > 0`, `available_stock >= 0`). 
  *Application validation can fail due to bugs or microservice discrepancies; DB CHECK constraints guarantee your database never holds invalid state.*
- **`UNIQUE`**: Prevents duplicate user accounts (`users.email`) and duplicate SKU identifiers (`products.sku`).

---

## 2. PostgreSQL Index Deep-Dive

### How B-Tree Indexes Work:
Without an index, finding a row requires a **Sequential Scan (Heap Scan)**: reading every 8KB disk page of the table from start to finish ($O(N)$ I/O).

A **B-Tree (Balanced Tree)** keeps indexed keys sorted in a multi-level tree of 8KB disk blocks:
```
           [ Root Page ]
          /      |      \
    [Branch]  [Branch]  [Branch]
     /   \     /   \     /   \
   [Leaf][Leaf][Leaf][Leaf][Leaf] -> Point to physical Tuple IDs (ctid: page_id, offset)
```
Finding a row takes $O(\log N)$ page reads (typically 3 to 4 disk lookups even for 50 million rows).

### Why Indexes Accelerate Reads but Hurt Writes:
Every single time an `INSERT`, `UPDATE`, or `DELETE` executes:
1. The row must be written to the table Heap file.
2. **EVERY SINGLE INDEX** on that table must be updated to insert a new key entry.
3. If an index leaf page is full (8KB), the database engine must execute a **B-Tree Page Split**:
   - Allocating a new disk page.
   - Moving half the keys to the new page.
   - Updating parent branch pointers.
   - Generating additional Write-Ahead Log (WAL) records.
4. On `UPDATE`: Modifying an indexed column marks the old index tuple dead and inserts a new one, causing index bloat.

> [!IMPORTANT]
> **Golden Rule of Indexing:** Only index columns appearing in high-frequency `WHERE` filters, `JOIN` conditions, and `ORDER BY` clauses. Unused indexes silently choke your database write throughput!

### Reading EXPLAIN & EXPLAIN ANALYZE:
- `EXPLAIN`: Shows the optimizer's estimated execution plan without running the query.
- `EXPLAIN ANALYZE`: Actually executes the query and reports real wall-clock milliseconds, loop counts, and memory usage.
- Look out for:
  - `Seq Scan on orders`: Full table scan. If table is large, consider an index!
  - `Index Scan`: Traverses B-Tree and fetches matching rows from the heap.
  - `Index Only Scan`: All requested columns exist directly in the index (no heap lookup needed — fastest!).

---

## 3. Pure JDBC Architecture

```
+-------------------------------------------------------+
|                 Application Code                      |
+-------------------------------------------------------+
                           |
+-------------------------------------------------------+
|   java.sql.* Interfaces (Connection, PreparedStatement)|
+-------------------------------------------------------+
                           |
+-------------------------------------------------------+
|   org.postgresql.Driver (JDBC Driver Implementation)  |
+-------------------------------------------------------+
                           |  (PostgreSQL Wire Protocol over TCP)
+-------------------------------------------------------+
|               PostgreSQL Server (Port 5432)           |
+-------------------------------------------------------+
```

### Pure Low-Level Workflow:
1. **`Connection`**: Represents an active TCP socket to PostgreSQL.
2. **`PreparedStatement`**:
   - Sends SQL query structure with `?` parameter markers to the database engine.
   - Database pre-compiles and parses the Abstract Syntax Tree (AST).
   - Input parameters are bound as strictly literal data values, eliminating SQL Injection entirely.
3. **`ResultSet`**: A cursor pointing to rows streamed from the database. Advances with `rs.next()`.
4. **`AutoCloseable` & Try-With-Resources**:
   Every `ResultSet`, `Statement`, and `Connection` consumes native OS file descriptors and server memory. Always declare them in `try (...)` blocks.

---

## 4. Manual Transaction Management (ACID)

In JDBC, connections start in **Auto-Commit** mode (`autoCommit = true`). Every executed statement is committed immediately and irreversibly.

To execute atomic business operations (like OrderFlow checkout):
```java
Connection conn = dataSource.getConnection();
try {
    // 1. Begin atomic transaction
    conn.setAutoCommit(false);

    // 2. Step 1: Reserve stock
    productRepo.reserveStock(productId, qty, conn);

    // 3. Step 2: Insert order header
    orderRepo.saveOrder(order, conn);

    // 4. Step 3: Insert order items
    orderRepo.saveItems(items, conn);

    // 5. Commit all changes atomically
    conn.commit();
} catch (Exception e) {
    // 6. Rollback everything on failure
    conn.rollback();
    throw e;
} finally {
    // 7. Reset autoCommit and return connection to pool
    conn.setAutoCommit(true);
    conn.close();
}
```

---

## 5. Why Connection Pooling (HikariCP) is Mandatory

- **The Problem with `DriverManager.getConnection()`:**
  Establishing a physical TCP connection requires a 3-way TCP handshake, SSL negotiation, credential verification, and backend server process initialization (15ms - 50ms per connection).
- **The Solution:**
  HikariCP maintains a pre-warmed pool of live connections.
  Borrowing a connection takes **sub-millisecond** time (~0.001ms).
  When you call `conn.close()`, the connection is **NOT terminated**; it is returned to the pool for reuse.
