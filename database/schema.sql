-- ==============================================================================
-- OrderFlow - E-Commerce Database Schema
-- Database: orderflow
-- Target: PostgreSQL 14+ / ANSI SQL Compatible
-- ==============================================================================
-- DESIGN PHILOSOPHY:
-- 1. Database-level constraints are the LAST LINE OF DEFENSE against data corruption.
--    Even if application code has bugs or multiple microservices access the DB,
--    data integrity cannot be compromised.
-- 2. Normalization (Third Normal Form - 3NF):
--    - 1NF: Atomic columns (no lists/arrays in single cells)
--    - 2NF: No partial dependency on composite keys
--    - 3NF: No transitive dependencies (e.g., store product unit_price in order_items
--      explicitly to preserve historical price at moment of purchase, avoiding lookup mutations).
-- ==============================================================================

-- Clean up existing tables (drop child tables first to respect FK dependencies)
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS inventory CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Optional: Drop custom ENUM types if recreating
DROP TYPE IF EXISTS order_status_enum CASCADE;
DROP TYPE IF EXISTS user_role_enum CASCADE;

-- Custom ENUM types enforce valid domain values at the storage engine level
CREATE TYPE user_role_enum AS ENUM ('CUSTOMER', 'ADMIN', 'MANAGER');
CREATE TYPE order_status_enum AS ENUM ('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED');

-- -----------------------------------------------------------------------------
-- 1. USERS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

COMMENT ON TABLE users IS 'Registered customers and administrative accounts';
COMMENT ON COLUMN users.email IS 'Unique login identifier, validated with email regex constraint';

-- -----------------------------------------------------------------------------
-- 2. PRODUCTS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_product_price_positive CHECK (price >= 0.01),
    CONSTRAINT chk_product_sku_not_empty CHECK (LENGTH(TRIM(sku)) > 0)
);

COMMENT ON TABLE products IS 'Master catalog of sellable merchandise';
COMMENT ON COLUMN products.price IS 'Monetary value represented as NUMERIC(12,2) to eliminate floating-point rounding errors';

-- -----------------------------------------------------------------------------
-- 3. INVENTORY TABLE
-- -----------------------------------------------------------------------------
-- Why separate products and inventory?
-- Separation of Concerns: Products store catalog metadata (marketing/display),
-- while inventory represents physical warehouse logistics. In multi-warehouse
-- setups, a product has multiple inventory rows.
CREATE TABLE inventory (
    product_id BIGINT PRIMARY KEY,
    available_stock INT NOT NULL DEFAULT 0,
    reserved_stock INT NOT NULL DEFAULT 0,
    warehouse_location VARCHAR(100) DEFAULT 'MAIN-WH-01',
    last_restocked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,

    -- Constraints: Stock levels can never be negative
    CONSTRAINT chk_inventory_stock_non_negative CHECK (available_stock >= 0),
    CONSTRAINT chk_inventory_reserved_non_negative CHECK (reserved_stock >= 0)
);

COMMENT ON TABLE inventory IS 'Tracks warehouse stock levels and reservations';

-- -----------------------------------------------------------------------------
-- 4. ORDERS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    shipping_address TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE RESTRICT,

    -- Constraints
    CONSTRAINT chk_orders_total_non_negative CHECK (total_amount >= 0.00),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

COMMENT ON TABLE orders IS 'Customer orders header tracking customer, status, and aggregate cost';

-- -----------------------------------------------------------------------------
-- 5. ORDER_ITEMS TABLE
-- -----------------------------------------------------------------------------
-- Represents line items in a shopping order (Many-to-Many resolution between orders and products)
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,

    -- Foreign Keys
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE RESTRICT,

    -- Constraints
    CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price_positive CHECK (unit_price >= 0.00),
    CONSTRAINT chk_order_items_subtotal_calc CHECK (subtotal = (quantity * unit_price)),
    CONSTRAINT uq_order_product UNIQUE (order_id, product_id)
);

COMMENT ON TABLE order_items IS 'Line items belonging to an order. Preserves unit_price at time of purchase.';

-- ==============================================================================
-- INDEX STRATEGY (CRUCIAL LEARNING SECTION)
-- ==============================================================================
-- WHY CREATE INDEXES?
-- 1. Foreign keys are NOT automatically indexed in PostgreSQL! Without an index on
--    order_id or user_id, queries joining users -> orders or orders -> order_items
--    must perform full table scans (Sequential Scans).
-- 2. An index creates a B-Tree structure allowing O(log N) lookup instead of O(N).
-- 
-- WHY DO INDEXES HURT WRITES?
-- Every time an INSERT, UPDATE, or DELETE occurs, the database engine must:
--   a) Write the tuple to the table heap file.
--   b) Navigate each B-Tree index and insert/update index nodes.
--   c) Handle possible B-Tree page splits when index pages fill up.
-- Therefore: ONLY index columns that are frequently filtered (WHERE), joined (JOIN),
-- or ordered (ORDER BY). Over-indexing drastically degrades write throughput.

-- Index for finding orders by customer
CREATE INDEX idx_orders_user_id ON orders(user_id);

-- Index for searching orders by status (e.g., querying all PENDING orders to process)
CREATE INDEX idx_orders_status ON orders(status);

-- Composite index for fast date-range filtering of orders by user
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

-- Index for retrieving order items for a specific order
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- Partial index: only index active products by category to keep index small and fast
CREATE INDEX idx_products_category_active ON products(category) WHERE is_active = TRUE;

-- ==============================================================================
-- 6. TRANSACTIONAL OUTBOX PATTERN & CONSUMER IDEMPOTENCY (PHASE 4)
-- ==============================================================================
-- WHY OUTBOX TABLE?
-- Solves the "Dual-Write Problem": In a distributed architecture, saving an Order
-- to PostgreSQL and publishing an event to Kafka cannot happen in one distributed ACID transaction
-- without complex 2PC (Two-Phase Commit).
-- Solution: Save the Order AND the Outbox record in the SAME local database transaction!
-- If DB commits, both are saved. If DB rolls back, neither is saved. Zero event loss!

CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);

-- WHY PROCESSED_EVENTS TABLE?
-- Solves "At-Least-Once Delivery": Kafka guarantees messages won't be lost, but
-- network partitions or consumer rebalances can cause a consumer to receive the
-- SAME message twice! An idempotent consumer checks this table before processing.

CREATE TABLE processed_events (
    event_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, consumer_name)
);
