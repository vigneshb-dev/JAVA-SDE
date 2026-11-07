-- ==============================================================================
-- OrderFlow - Database Seed Data
-- ==============================================================================

-- Clear existing data
TRUNCATE TABLE order_items, orders, inventory, products, users RESTART IDENTITY CASCADE;

-- -----------------------------------------------------------------------------
-- 1. SEED USERS
-- -----------------------------------------------------------------------------
INSERT INTO users (email, password_hash, full_name, role, status, created_at)
VALUES 
    ('alice.johnson@example.com', '$2a$12$e8Y6L2Wp9k7J9...hashedAlice', 'Alice Johnson', 'CUSTOMER', 'ACTIVE', '2026-01-10 09:30:00+00'),
    ('bob.smith@example.com', '$2a$12$xP2jL3Nq1b5C8...hashedBob', 'Bob Smith', 'CUSTOMER', 'ACTIVE', '2026-01-15 14:22:00+00'),
    ('charlie.brown@example.com', '$2a$12$kL9mO4Rt7v2X1...hashedCharlie', 'Charlie Brown', 'CUSTOMER', 'SUSPENDED', '2026-02-01 11:15:00+00'),
    ('diana.prince@example.com', '$2a$12$yT5vB8Nm2x4Z6...hashedDiana', 'Diana Prince', 'CUSTOMER', 'ACTIVE', '2026-02-14 16:45:00+00'),
    ('admin@orderflow.internal', '$2a$12$zX8cV7Bn1m3K5...hashedAdmin', 'Master Administrator', 'ADMIN', 'ACTIVE', '2026-01-01 00:00:00+00'),
    ('inventory.manager@orderflow.internal', '$2a$12$qW4eR7Ty2u9I3...hashedMgr', 'Sarah Connor', 'MANAGER', 'ACTIVE', '2026-01-05 08:00:00+00');

-- -----------------------------------------------------------------------------
-- 2. SEED PRODUCTS
-- -----------------------------------------------------------------------------
INSERT INTO products (sku, name, description, price, category, is_active)
VALUES
    ('TECH-KEY-001', 'Mechanical Keyboard Pro', 'Tenkeyless RGB mechanical keyboard with brown switches', 129.99, 'ELECTRONICS', TRUE),
    ('TECH-MOU-002', 'Wireless Precision Mouse', 'Ergonomic 4000 DPI wireless mouse with rechargeable battery', 49.99, 'ELECTRONICS', TRUE),
    ('TECH-MON-003', '27-inch 4K IPS Monitor', 'Ultra HD 60Hz monitor with USB-C 65W charging', 349.50, 'ELECTRONICS', TRUE),
    ('FURN-DSK-004', 'Motorized Standing Desk', 'Electric height-adjustable desk with dual motors (140x70cm)', 499.00, 'FURNITURE', TRUE),
    ('FURN-CHR-005', 'Ergonomic Mesh Chair', 'High-back desk chair with adjustable lumbar support', 280.00, 'FURNITURE', TRUE),
    ('BOOK-DSN-006', 'Designing Data-Intensive Apps', 'Reliable, scalable, and maintainable systems guide by Martin Kleppmann', 45.00, 'BOOKS', TRUE),
    ('BOOK-CLD-007', 'Clean Architecture', 'A craftsman guide to software structure by Robert C. Martin', 38.50, 'BOOKS', TRUE),
    ('ACC-CAB-008', 'Braided USB-C Cable (2m)', '100W Power Delivery durable nylon braided cable', 14.99, 'ACCESSORIES', TRUE),
    ('ACC-HUB-009', '7-in-1 USB-C Hub', 'HDMI 4K, SD reader, USB 3.0 x3, 100W PD pass-through', 59.99, 'ACCESSORIES', FALSE); -- Inactive product

-- -----------------------------------------------------------------------------
-- 3. SEED INVENTORY
-- -----------------------------------------------------------------------------
INSERT INTO inventory (product_id, available_stock, reserved_stock, warehouse_location)
VALUES
    (1, 45, 5, 'WH-A-RACK-01'),  -- Mechanical Keyboard
    (2, 80, 10, 'WH-A-RACK-02'), -- Precision Mouse
    (3, 15, 2, 'WH-B-RACK-10'),  -- 4K Monitor
    (4, 8, 1, 'WH-C-BULK-01'),   -- Standing Desk
    (5, 12, 0, 'WH-C-BULK-02'),  -- Mesh Chair
    (6, 120, 0, 'WH-D-SHELF-01'), -- Designing Data-Intensive Apps
    (7, 95, 0, 'WH-D-SHELF-02'),  -- Clean Architecture
    (8, 250, 20, 'WH-A-BIN-05'),  -- USB-C Cable
    (9, 0, 0, 'WH-A-BIN-06');     -- Inactive Hub (Out of Stock)

-- -----------------------------------------------------------------------------
-- 4. SEED ORDERS
-- -----------------------------------------------------------------------------
INSERT INTO orders (id, order_number, user_id, status, total_amount, shipping_address, created_at)
VALUES
    (1001, 'ORD-202602-0001', 1, 'DELIVERED', 179.98, '123 Market St, San Francisco, CA 94103', '2026-02-10 10:15:00+00'),
    (1002, 'ORD-202602-0002', 2, 'PAID', 848.50, '456 Oak Avenue, Austin, TX 78701', '2026-02-12 14:40:00+00'),
    (1003, 'ORD-202602-0003', 1, 'SHIPPED', 83.50, '123 Market St, San Francisco, CA 94103', '2026-02-15 16:20:00+00'),
    (1004, 'ORD-202602-0004', 4, 'PENDING', 29.98, '789 Pine Road, Seattle, WA 98101', '2026-02-18 09:05:00+00');

-- -----------------------------------------------------------------------------
-- 5. SEED ORDER_ITEMS
-- -----------------------------------------------------------------------------
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
VALUES
    -- Order 1001: Keyboard + Mouse (Total: 129.99 + 49.99 = 179.98)
    (1001, 1, 1, 129.99, 129.99),
    (1001, 2, 1, 49.99, 49.99),

    -- Order 1002: Monitor + Standing Desk (Total: 349.50 + 499.00 = 848.50)
    (1002, 3, 1, 349.50, 349.50),
    (1002, 4, 1, 499.00, 499.00),

    -- Order 1003: Kleppmann Book + Clean Architecture (Total: 45.00 + 38.50 = 83.50)
    (1003, 6, 1, 45.00, 45.00),
    (1003, 7, 1, 38.50, 38.50),

    -- Order 1004: 2x USB-C Cables (Total: 2 * 14.99 = 29.98)
    (1004, 8, 2, 14.99, 29.98);

-- Reset sequence for tables using auto-increment primary keys
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('order_items_id_seq', (SELECT MAX(id) FROM order_items));
