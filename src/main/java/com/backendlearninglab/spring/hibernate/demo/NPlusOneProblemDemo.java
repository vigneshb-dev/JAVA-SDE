package com.backendlearninglab.spring.hibernate.demo;

import com.backendlearninglab.spring.entity.OrderEntity;
import com.backendlearninglab.spring.repository.SpringDataOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HIBERNATE PERFORMANCE: The N+1 Query Problem & Production Solutions.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li><b>WHAT CAUSES THE N+1 PROBLEM:</b>
 *       When an entity has a {@code FetchType.LAZY} association (e.g. OrderEntity -> items),
 *       calling {@code orderRepository.findAll()} executes 1 query to fetch all N orders.
 *       When business code later loops over the orders and calls {@code order.getItems().size()},
 *       Hibernate intercepts the call through a ByteBuddy dynamic proxy and executes a separate
 *       SQL query for <i>each individual order</i>!
 *       <pre>
 *         Query 1: SELECT * FROM orders;                      -- 1 query returning 100 orders
 *         Query 2: SELECT * FROM order_items WHERE order_id=1; -- +1
 *         Query 3: SELECT * FROM order_items WHERE order_id=2; -- +1
 *         ...
 *         Query 101: SELECT * FROM order_items WHERE order_id=100; -- +1
 *         Total: 1 + 100 = 101 SQL queries sent over the network!
 *       </pre>
 *   </li>
 *   <li><b>HOW TO FIX IT:</b>
 *       <ol>
 *         <li><b>JOIN FETCH:</b> An explicit JPQL query ({@code SELECT o FROM OrderEntity o JOIN FETCH o.items})
 *             instructs Hibernate to issue an SQL {@code LEFT JOIN}, fetching orders and items in a single query.</li>
 *         <li><b>@EntityGraph:</b> A declarative Spring Data annotation specifying paths to fetch eagerly
 *             for a specific query, leaving the domain entity default as LAZY.</li>
 *       </ol>
 *   </li>
 *   <li><b>TRADEOFFS & WARNINGS:</b>
 *       <ul>
 *         <li>Never join fetch two different {@code @OneToMany} collections in the same query;
 *             it creates a Cartesian Product (e.g. 10 items * 10 payments = 100 duplicate rows)
 *             and Hibernate throws {@code MultipleBagFetchException}!</li>
 *         <li>Paging (LIMIT/OFFSET) with join fetch on collections forces Hibernate to paginate
 *             in JVM memory (HHH000104 warning), risking OutOfMemoryError!</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Component
public class NPlusOneProblemDemo {

    private static final Logger log = LoggerFactory.getLogger(NPlusOneProblemDemo.class);

    private final SpringDataOrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public NPlusOneProblemDemo(SpringDataOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Demonstrates the N+1 problem: Observe the console logs!
     * You will see 1 query for orders, followed by separate queries for each order's items.
     */
    @Transactional(readOnly = true)
    public void demonstrateTheNPlusOneProblem() {
        log.warn("=== [N+1 PROBLEM DEMO] Executing findAll() with LAZY loading ===");

        // Query 1: Fetches all orders
        List<OrderEntity> orders = orderRepository.findAll();
        log.info("Query 1 finished. Fetched {} orders. Now iterating to access line items...", orders.size());

        // Queries 2..N+1: Triggered on each iteration!
        for (OrderEntity order : orders) {
            int itemCount = order.getItems().size(); // Triggers lazy load proxy query!
            log.info("  Order {} has {} items (Lazy query executed for this row!)", order.getOrderNumber(), itemCount);
        }
    }

    /**
     * Demonstrates the solution using JOIN FETCH:
     * Only 1 single SQL query with an INNER/LEFT JOIN is sent to PostgreSQL!
     */
    @Transactional(readOnly = true)
    public void demonstrateTheJoinFetchSolution(Long orderId) {
        log.info("=== [SOLUTION] Executing JOIN FETCH for Order ID: {} ===", orderId);
        orderRepository.findByIdWithItemsAndUserJoinFetch(orderId).ifPresent(order -> {
            log.info("Fetched Order {} with {} items in a SINGLE SQL statement!",
                    order.getOrderNumber(), order.getItems().size());
        });
    }
}
