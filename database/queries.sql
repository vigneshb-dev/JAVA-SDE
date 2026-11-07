-- ==============================================================================
-- OrderFlow - SQL Learning & Query Mastery
-- ==============================================================================
-- This script contains executable SQL queries demonstrating real-world backend
-- patterns, performance profiling, and transaction management.
-- ==============================================================================

-- -----------------------------------------------------------------------------
-- 1. BASIC FILTERING, PROJECTION, AND SORTING
-- -----------------------------------------------------------------------------
-- Find all active products priced between $30 and $200, sorted by price descending.
SELECT 
    sku,
    name,
    category,
    price
FROM products
WHERE is_active = TRUE 
  AND price BETWEEN 30.00 AND 200.00
ORDER BY price DESC;

-- -----------------------------------------------------------------------------
-- 2. STRING PATTERN MATCHING & COALESCE
-- -----------------------------------------------------------------------------
-- COALESCE returns the first non-null argument. Crucial for handling optional fields
-- without introducing NullPointerExceptions or ugly UI displays.
SELECT 
    name,
    sku,
    COALESCE(description, 'No description provided') AS display_description,
    price
FROM products
WHERE name ILIKE '%pro%' OR sku LIKE 'TECH-%';

-- -----------------------------------------------------------------------------
-- 3. INNER JOIN (Relational Composition)
-- -----------------------------------------------------------------------------
-- Retrieve order header details along with customer details.
-- Excludes orders without valid users (though FK enforces this) and users without orders.
SELECT 
    o.order_number,
    u.full_name AS customer_name,
    u.email AS customer_email,
    o.status AS order_status,
    o.total_amount,
    o.created_at
FROM orders o
INNER JOIN users u ON o.user_id = u.id
ORDER BY o.created_at DESC;

-- -----------------------------------------------------------------------------
-- 4. LEFT OUTER JOIN (Preserving Left Table Records)
-- -----------------------------------------------------------------------------
-- List EVERY user, and their order count if any.
-- If a user has never ordered (e.g. Diana Prince, Charlie Brown), order_number is NULL.
SELECT 
    u.id AS user_id,
    u.full_name,
    u.email,
    COUNT(o.id) AS total_orders,
    COALESCE(SUM(o.total_amount), 0.00) AS lifetime_spend
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.full_name, u.email
ORDER BY lifetime_spend DESC;

-- -----------------------------------------------------------------------------
-- 5. MULTI-TABLE JOIN (Order Details Report)
-- -----------------------------------------------------------------------------
-- Joins 4 tables: orders -> users -> order_items -> products
SELECT 
    o.order_number,
    u.full_name AS customer,
    p.sku,
    p.name AS product_name,
    oi.quantity,
    oi.unit_price,
    oi.subtotal
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
WHERE o.order_number = 'ORD-202602-0001';

-- -----------------------------------------------------------------------------
-- 6. SELF JOIN (Hierarchical or Comparative Analysis)
-- -----------------------------------------------------------------------------
-- Compare each product with other products in the same category that are cheaper.
SELECT 
    p1.category,
    p1.name AS product,
    p1.price AS product_price,
    p2.name AS cheaper_alternative,
    p2.price AS alternative_price,
    (p1.price - p2.price) AS price_difference
FROM products p1
JOIN products p2 
  ON p1.category = p2.category 
 AND p1.id != p2.id 
 AND p1.price > p2.price
ORDER BY p1.category, p1.price DESC;

-- -----------------------------------------------------------------------------
-- 7. AGGREGATION: GROUP BY & HAVING
-- -----------------------------------------------------------------------------
-- Find categories with more than 1 active product and average price over $50.
-- Rule: WHERE filters rows BEFORE aggregation; HAVING filters groups AFTER aggregation.
SELECT 
    category,
    COUNT(*) AS total_products,
    ROUND(AVG(price), 2) AS average_price,
    MIN(price) AS min_price,
    MAX(price) AS max_price
FROM products
WHERE is_active = TRUE
GROUP BY category
HAVING COUNT(*) > 1 AND AVG(price) > 50.00
ORDER BY average_price DESC;

-- -----------------------------------------------------------------------------
-- 8. CONDITIONAL LOGIC: CASE EXPRESSION
-- -----------------------------------------------------------------------------
-- Categorize customers by spending tier dynamically.
SELECT 
    u.full_name,
    COALESCE(SUM(o.total_amount), 0.00) AS total_spent,
    CASE 
        WHEN SUM(o.total_amount) >= 500 THEN 'VIP Platinum'
        WHEN SUM(o.total_amount) >= 100 THEN 'Gold Preferred'
        WHEN SUM(o.total_amount) > 0 THEN 'Active Shopper'
        ELSE 'Prospect (No Orders)'
    END AS customer_tier
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.full_name
ORDER BY total_spent DESC;

-- -----------------------------------------------------------------------------
-- 9. SUBQUERIES: SCALAR & CORRELATED
-- -----------------------------------------------------------------------------
-- Scalar Subquery: Find products priced higher than the overall catalog average.
SELECT 
    sku,
    name,
    price,
    (SELECT ROUND(AVG(price), 2) FROM products) AS catalog_avg_price
FROM products
WHERE price > (SELECT AVG(price) FROM products)
ORDER BY price DESC;

-- Correlated Subquery: Products priced higher than the average of THEIR OWN CATEGORY.
SELECT 
    p1.category,
    p1.name,
    p1.price
FROM products p1
WHERE p1.price > (
    SELECT AVG(p2.price)
    FROM products p2
    WHERE p2.category = p1.category
);

-- -----------------------------------------------------------------------------
-- 10. SUBQUERIES: IN vs. EXISTS
-- -----------------------------------------------------------------------------
-- Find all users who have placed at least one order.
-- IN approach:
SELECT id, full_name, email
FROM users
WHERE id IN (SELECT DISTINCT user_id FROM orders);

-- EXISTS approach (Generally faster on large datasets because the database engine
-- short-circuits as soon as a single matching record is found):
SELECT u.id, u.full_name, u.email
FROM users u
WHERE EXISTS (
    SELECT 1 
    FROM orders o 
    WHERE o.user_id = u.id
);

-- Find products that have NEVER been ordered (NOT EXISTS):
SELECT p.id, p.sku, p.name
FROM products p
WHERE NOT EXISTS (
    SELECT 1 
    FROM order_items oi 
    WHERE oi.product_id = p.id
);

-- -----------------------------------------------------------------------------
-- 11. DATA MODIFICATION: INSERT, UPDATE, DELETE & ON CONFLICT
-- -----------------------------------------------------------------------------
-- Safe Upsert (Insert or Update on Primary/Unique Key conflict)
INSERT INTO inventory (product_id, available_stock, reserved_stock, warehouse_location)
VALUES (1, 50, 5, 'WH-A-RACK-01')
ON CONFLICT (product_id) 
DO UPDATE SET 
    available_stock = inventory.available_stock + EXCLUDED.available_stock,
    last_restocked_at = CURRENT_TIMESTAMP;

-- Atomic inventory reservation (guarantees stock never drops below zero)
UPDATE inventory
SET 
    available_stock = available_stock - 2,
    reserved_stock = reserved_stock + 2
WHERE product_id = 1 
  AND available_stock >= 2; -- Critical predicate prevents race condition!

-- Delete inactive products with 0 stock that have never been ordered
DELETE FROM products
WHERE is_active = FALSE
  AND id NOT IN (SELECT DISTINCT product_id FROM order_items);

-- -----------------------------------------------------------------------------
-- 12. TRANSACTIONS & ACID GUARANTEES
-- -----------------------------------------------------------------------------
-- ACID:
-- A (Atomicity): All steps succeed or everything is rolled back.
-- C (Consistency): Database transitions from one valid state to another (constraints preserved).
-- I (Isolation): Concurrent transactions do not observe intermediate dirty states.
-- D (Durability): Committed data is written to Write-Ahead Log (WAL) and survives crashes.

BEGIN;

-- Step 1: Create new order
INSERT INTO orders (order_number, user_id, status, total_amount, shipping_address)
VALUES ('ORD-202602-9999', 1, 'PENDING', 49.99, '123 Market St')
RETURNING id;

-- Create a savepoint
SAVEPOINT order_created;

-- Step 2: Insert order item
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
VALUES (currval('orders_id_seq'), 2, 1, 49.99, 49.99);

-- Step 3: Deduct inventory
UPDATE inventory
SET available_stock = available_stock - 1,
    reserved_stock = reserved_stock + 1
WHERE product_id = 2 AND available_stock >= 1;

-- If an error occurred here, we could run: ROLLBACK TO SAVEPOINT order_created;
-- Or to cancel everything: ROLLBACK;

COMMIT;

-- -----------------------------------------------------------------------------
-- 13. PERFORMANCE PROFILING: EXPLAIN & EXPLAIN ANALYZE
-- -----------------------------------------------------------------------------
-- EXPLAIN: Shows the query execution plan generated by the optimizer without running it.
-- EXPLAIN ANALYZE: Executes the query, measures actual wall-clock execution time,
-- loop counts, and memory usage.

-- Query using index on orders(user_id):
EXPLAIN ANALYZE
SELECT * FROM orders WHERE user_id = 1;

-- Query on unindexed column (forces Sequential Scan across the entire table):
EXPLAIN ANALYZE
SELECT * FROM orders WHERE shipping_address LIKE '%Market St%';

-- ==============================================================================
-- DEEP DIVE: WHY INDEXES HELP READS BUT HURT WRITES
-- ==============================================================================
-- 1. READS:
--    A table without an index is stored as a Heap File (unsorted tuples).
--    Finding a record requires O(N) Sequential Scan (reading every disk block).
--    With a B-Tree Index: Keys are kept in balanced tree order.
--    Finding a record requires O(log N) page reads (typically 3 to 4 I/O reads).
--
-- 2. WRITES (INSERT, UPDATE, DELETE):
--    - When you INSERT a row into `orders`, the row is written to the table heap.
--    - In addition, EVERY index on `orders` (idx_orders_user_id, idx_orders_status,
--      idx_orders_user_created, PK) must have a new entry inserted into its B-Tree.
--    - If a B-Tree page (node) is full (typically 8KB), it must SPLIT into two pages.
--      Page splits cause heavy random disk I/O and generate additional Write-Ahead
--      Log (WAL) traffic.
--    - On UPDATE: If an indexed column changes, the old index entry must be marked
--      dead and the new entry inserted into a different B-Tree leaf.
--
-- CONCLUSION:
-- Never blindly index every column. Measure query frequency, use EXPLAIN ANALYZE,
-- and balance read acceleration against write ingestion throughput.
