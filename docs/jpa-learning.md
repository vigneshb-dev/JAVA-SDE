# JPA (Jakarta Persistence API) Architecture & Engineering Guide

---

## 1. Concept: Object-Relational Impedance Mismatch

Object-oriented programming represents data as **graphs of interconnected objects** featuring inheritance, encapsulation, and identity references (`order.getItems()`). 
Relational databases represent data as **flat tabular tuples** linked by scalar foreign keys (`order_id = 101`).

This fundamental friction is called the **Object-Relational Impedance Mismatch**.
JPA (Jakarta Persistence API) is the official specification that standardizes how Java entities, relationships, primary keys, and queries map onto relational tables.

---

## 2. Small Isolated Example: Proper Association Mapping

```java
@Entity
@Table(name = "authors")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // INVERSE SIDE (mappedBy indicates 'author' field on Book owns the FK)
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();
}

@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // OWNING SIDE (holds the foreign key column 'author_id')
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;
}
```

---

## 3. OrderFlow Project Implementation

Examine:
- [OrderEntity.java](file:///g:/JAVA%20SDE/src/main/java/com/backendlearninglab/spring/entity/OrderEntity.java)
- [OrderItemEntity.java](file:///g:/JAVA%20SDE/src/main/java/com/backendlearninglab/spring/entity/OrderItemEntity.java)
- [ProductEntity.java](file:///g:/JAVA%20SDE/src/main/java/com/backendlearninglab/spring/entity/ProductEntity.java)

Key architectural patterns used:
1. **Owning vs. Inverse Sides:** `OrderItemEntity` owns `@JoinColumn(name = "order_id")`. Any change to line items updates the database via this FK.
2. **Cascade & Orphan Removal:** `order.removeItem(item)` in Java automatically triggers a SQL `DELETE FROM order_items WHERE id = ?`.
3. **Synchronization Helper Methods:** `order.addItem(item)` sets both `order.items.add(item)` AND `item.setOrder(order)` so the in-memory object graph remains consistent before database flush.

---

## 4. Common Mistake: Defaulting to EAGER Fetching

```java
// CRITICAL PERFORMANCE MISTAKE:
@ManyToOne // In JPA, @ManyToOne and @OneToOne default to FetchType.EAGER!
private ProductEntity product;

// THE CONSEQUENCE:
// Querying 1,000 order items forces Hibernate to issue 1,000 eager JOINs or SELECTs,
// dragging the entire product catalog into memory.

// THE FIX:
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "product_id", nullable = false)
private ProductEntity product;
```

---

## 5. Debugging Method: How to Detect Unexpected Queries

1. Enable SQL Logging in `application.properties`:
   ```properties
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true
   logging.level.org.hibernate.SQL=DEBUG
   logging.level.org.hibernate.orm.jdbc.bind=TRACE
   ```
2. Inspect the console output:
   - If calling a single `findById()` triggers 15 SQL queries, you have an un-configured `EAGER` association!

---

## 6. Performance Consideration: Bidirectional equals() & hashCode()

When using bidirectional associations:
- Never include `@OneToMany` or `@ManyToOne` fields inside `equals()`, `hashCode()`, or `toString()`.
- Doing so creates an infinite loop: `Order.toString() -> OrderItem.toString() -> Order.toString()`, instantly crashing your application with a `StackOverflowError`!
- For JPA entities, base `equals()` on identity (`id != null && id.equals(that.id)`) and return a stable `getClass().hashCode()`.

---

## 7. Senior Interview Question

**Question:** *"What is the difference between `CascadeType.REMOVE` and `orphanRemoval = true` in JPA?"*

**Answer:**
- **`CascadeType.REMOVE`:** If the *parent* entity is deleted (`em.remove(order)`), all child entities (`order_items`) are also deleted. However, if you simply remove a child from the parent's collection (`order.getItems().remove(0)`), `CascadeType.REMOVE` does **NOT** delete the child row from the database (it becomes an orphaned foreign key or sets FK to null).
- **`orphanRemoval = true`:** Deleting the parent deletes children, AND removing a child from the parent's collection in Java explicitly tells Hibernate to issue a SQL `DELETE` for that orphaned child row!
