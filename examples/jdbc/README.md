# Pure JDBC & Relational Database Demos

The runnable JDBC laboratories are located in `src/main/java/com/backendlearninglab/jdbc/`:

| Module | Location | Concepts Covered |
| :--- | :--- | :--- |
| **SQL Injection & Statements** | `jdbc/JdbcBasicsAndSqlInjectionDemo.java` | Statement string concatenation vs PreparedStatement parameterized queries; proving how `PreparedStatement` pre-compiles the AST (Abstract Syntax Tree) to render SQL injection impossible. |
| **Resource Leaks** | `jdbc/JdbcResourceLeakDemo.java` | Unclosed `ResultSet`, `Statement`, and `Connection` exhausting TCP sockets and HikariCP connection pools; refactoring to nested try-with-resources. |
| **ACID & Batch Operations** | `jdbc/JdbcTransactionAndBatchDemo.java` | `setAutoCommit(false)`, `commit()`, `rollback()`, and batch insertion (`addBatch()`, `executeBatch()`) reducing network roundtrips from $O(N)$ to $O(1)$. |
| **Manual Service Layer** | `service/OrderService.java` | Manual ACID transaction coordinating customer verification, product lookup, stock deduction, and order placement with rollback safety. |

### How to Run Any JDBC Demo:
```bash
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.jdbc.JdbcBasicsAndSqlInjectionDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.jdbc.JdbcResourceLeakDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.jdbc.JdbcTransactionAndBatchDemo"
mvn exec:java "-Dexec.mainClass=com.backendlearninglab.OrderFlowApplication"
```
