package com.backendlearninglab.spring.hibernate.demo;

import com.backendlearninglab.spring.entity.ProductEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * HIBERNATE INTERNALS: Entity Lifecycle, Persistence Context, and Dirty Checking.
 * <p>
 * TEACHING FOCUS:
 * <ul>
 *   <li><b>THE STACK ARCHITECTURE:</b>
 *       <pre>
 *         Application Code
 *              │
 *              ▼
 *         JPA Specification (jakarta.persistence.* interfaces: EntityManager, Entity, Transaction)
 *              │
 *              ▼
 *         Hibernate ORM (Implementation: Session, PersistenceContext, Bytecode enhancement)
 *              │
 *              ▼
 *         JDBC (java.sql.Connection, PreparedStatement, ResultSet)
 *              │
 *              ▼
 *         PostgreSQL Driver & Database Engine
 *       </pre>
 *   </li>
 *   <li><b>THE 4 ENTITY LIFECYCLE STATES:</b>
 *       <ol>
 *         <li><b>Transient:</b> Created via {@code new ProductEntity()}. Heap object only. No DB identity, not in First-Level Cache.</li>
 *         <li><b>Managed:</b> Associated with an active {@link EntityManager} and Persistence Context. Changes are tracked.</li>
 *         <li><b>Detached:</b> Has database ID, but the Persistence Context/Transaction has closed. Modifications do NOT update DB.</li>
 *         <li><b>Removed:</b> Marked for deletion. Hibernate will issue a SQL {@code DELETE} upon transaction commit.</li>
 *       </ol>
 *   </li>
 *   <li><b>DIRTY CHECKING (WHY SQL RUNS WITHOUT WRITING SQL):</b>
 *       When an entity is loaded into the Persistence Context, Hibernate creates a hidden snapshot copy of its initial state.
 *       At transaction commit time (during {@code em.flush()}), Hibernate performs <i>dirty checking</i>:
 *       it compares the entity's current property values against the initial snapshot.
 *       If any field has mutated, Hibernate automatically builds and executes a parameterized SQL {@code UPDATE} statement!
 *   </li>
 * </ul>
 */
@Component
public class EntityLifecycleAndDirtyCheckingDemo {

    private static final Logger log = LoggerFactory.getLogger(EntityLifecycleAndDirtyCheckingDemo.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Demonstrates the transition from Transient -> Managed -> Dirty Checking -> Detached.
     */
    @Transactional
    public void demonstrateLifecycleAndDirtyChecking() {
        log.info("=================================================================");
        log.info("   HIBERNATE DEMO: ENTITY LIFECYCLE & DIRTY CHECKING             ");
        log.info("=================================================================");

        // 1. TRANSIENT STATE:
        // A standard Java POJO in RAM. Not known to Hibernate.
        ProductEntity product = new ProductEntity("TECH-DEMO-99", "Demo Gaming Headset", "Surround Sound",
                new BigDecimal("99.99"), "ELECTRONICS");
        log.info("1. Transient State: product.id is {}", product.getId()); // null

        // 2. MANAGED STATE:
        // Calling persist() registers the entity in the Persistence Context (First-Level Cache)
        entityManager.persist(product);
        log.info("2. Managed State: After persist(), product.id is {}", product.getId());

        // 3. FIRST-LEVEL CACHE LOOKUP:
        // Querying for the same ID within the same transaction does NOT execute SQL SELECT!
        // Hibernate retrieves the instance directly from its in-memory First-Level Cache.
        ProductEntity cachedProduct = entityManager.find(ProductEntity.class, product.getId());
        log.info("3. First-Level Cache check: (product == cachedProduct) -> {}", (product == cachedProduct)); // true!

        // 4. DIRTY CHECKING DEMONSTRATION:
        // We modify a field using a plain Java setter.
        // NOTICE: We NEVER call entityManager.merge() or any repository.save()!
        log.info("4. Mutating price from $99.99 to $79.99 via Java setter...");
        product.setPrice(new BigDecimal("79.99"));

        // When this method exits, @Transactional commits.
        // Hibernate's flush() runs dirty checking, detects the price difference,
        // and automatically executes:
        //   UPDATE products SET price = 79.99, ... WHERE id = ?
        log.info("5. Transaction commit will now flush dirty state to database!");
    }
}
