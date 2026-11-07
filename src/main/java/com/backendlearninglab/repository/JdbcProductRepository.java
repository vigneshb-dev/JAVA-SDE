package com.backendlearninglab.repository;

import com.backendlearninglab.exception.DatabaseOperationException;
import com.backendlearninglab.model.Inventory;
import com.backendlearninglab.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure JDBC implementation of {@link ProductRepository}.
 */
public class JdbcProductRepository implements ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcProductRepository.class);
    private final DataSource dataSource;

    public JdbcProductRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            return insert(product);
        } else {
            return update(product);
        }
    }

    private Product insert(Product product) {
        String sql = "INSERT INTO products (sku, name, description, price, category, is_active, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            Instant now = Instant.now();
            pstmt.setString(1, product.getSku());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getDescription());
            pstmt.setBigDecimal(4, product.getPrice());
            pstmt.setString(5, product.getCategory());
            pstmt.setBoolean(6, product.isActive());
            pstmt.setTimestamp(7, Timestamp.from(now));
            pstmt.setTimestamp(8, Timestamp.from(now));

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    product.setId(keys.getLong(1));
                    product.setCreatedAt(now);
                    product.setUpdatedAt(now);
                } else {
                    throw new DatabaseOperationException("Failed to retrieve generated ID for product");
                }
            }
            return product;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error inserting product: " + product.getSku(), e);
        }
    }

    private Product update(Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, category = ?, is_active = ?, updated_at = ? " +
                     "WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Instant now = Instant.now();
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setBigDecimal(3, product.getPrice());
            pstmt.setString(4, product.getCategory());
            pstmt.setBoolean(5, product.isActive());
            pstmt.setTimestamp(6, Timestamp.from(now));
            pstmt.setLong(7, product.getId());

            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw new DatabaseOperationException("Product not found to update, id: " + product.getId());
            }
            product.setUpdatedAt(now);
            return product;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating product ID: " + product.getId(), e);
        }
    }

    @Override
    public Optional<Product> findById(Long id) {
        String sql = "SELECT id, sku, name, description, price, category, is_active, created_at, updated_at " +
                     "FROM products WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProduct(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying product by ID: " + id, e);
        }
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        String sql = "SELECT id, sku, name, description, price, category, is_active, created_at, updated_at " +
                     "FROM products WHERE sku = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sku.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProduct(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying product by SKU: " + sku, e);
        }
    }

    @Override
    public List<Product> findAllActive() {
        String sql = "SELECT id, sku, name, description, price, category, is_active, created_at, updated_at " +
                     "FROM products WHERE is_active = TRUE ORDER BY name";
        List<Product> products = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
            return products;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying active products", e);
        }
    }

    @Override
    public List<Product> findByCategory(String category) {
        String sql = "SELECT id, sku, name, description, price, category, is_active, created_at, updated_at " +
                     "FROM products WHERE category = ? AND is_active = TRUE ORDER BY price ASC";
        List<Product> products = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRowToProduct(rs));
                }
            }
            return products;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying products by category: " + category, e);
        }
    }

    @Override
    public boolean updatePrice(Long id, BigDecimal newPrice) {
        String sql = "UPDATE products SET price = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, newPrice);
            pstmt.setTimestamp(2, Timestamp.from(Instant.now()));
            pstmt.setLong(3, id);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating price for product ID: " + id, e);
        }
    }

    @Override
    public void saveInventory(Inventory inventory) {
        // Universal Upsert Pattern: Try UPDATE first, if 0 rows affected, execute INSERT.
        // This is 100% portable across PostgreSQL, H2, Oracle, MySQL and SQLite!
        String updateSql = "UPDATE inventory SET available_stock = ?, reserved_stock = ?, warehouse_location = ?, last_restocked_at = ? " +
                           "WHERE product_id = ?";
        String insertSql = "INSERT INTO inventory (product_id, available_stock, reserved_stock, warehouse_location, last_restocked_at) " +
                           "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            Timestamp restockedTs = Timestamp.from(inventory.getLastRestockedAt() != null ? inventory.getLastRestockedAt() : Instant.now());

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, inventory.getAvailableStock());
                updateStmt.setInt(2, inventory.getReservedStock());
                updateStmt.setString(3, inventory.getWarehouseLocation());
                updateStmt.setTimestamp(4, restockedTs);
                updateStmt.setLong(5, inventory.getProductId());

                int affected = updateStmt.executeUpdate();
                if (affected == 0) {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setLong(1, inventory.getProductId());
                        insertStmt.setInt(2, inventory.getAvailableStock());
                        insertStmt.setInt(3, inventory.getReservedStock());
                        insertStmt.setString(4, inventory.getWarehouseLocation());
                        insertStmt.setTimestamp(5, restockedTs);
                        insertStmt.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving inventory for product ID: " + inventory.getProductId(), e);
        }
    }

    @Override
    public Optional<Inventory> findInventoryByProductId(Long productId) {
        String sql = "SELECT product_id, available_stock, reserved_stock, warehouse_location, last_restocked_at " +
                     "FROM inventory WHERE product_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_restocked_at");
                    return Optional.of(new Inventory(
                            rs.getLong("product_id"),
                            rs.getInt("available_stock"),
                            rs.getInt("reserved_stock"),
                            rs.getString("warehouse_location"),
                            ts != null ? ts.toInstant() : Instant.now()
                    ));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying inventory for product ID: " + productId, e);
        }
    }

    /**
     * Atomically reserves stock in the inventory table.
     * Uses the supplied connection so it can be part of an external business transaction!
     */
    @Override
    public boolean reserveStock(Long productId, int quantity, Connection conn) {
        String sql = "UPDATE inventory " +
                     "SET available_stock = available_stock - ?, " +
                     "    reserved_stock = reserved_stock + ? " +
                     "WHERE product_id = ? AND available_stock >= ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, quantity);
            pstmt.setLong(3, productId);
            pstmt.setInt(4, quantity); // CRITICAL: Ensures atomic check-and-decrement!

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error reserving stock for product ID: " + productId, e);
        }
    }

    @Override
    public boolean restoreStock(Long productId, int quantity, Connection conn) {
        String sql = "UPDATE inventory " +
                     "SET available_stock = available_stock + ?, " +
                     "    reserved_stock = reserved_stock - ? " +
                     "WHERE product_id = ? AND reserved_stock >= ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, quantity);
            pstmt.setLong(3, productId);
            pstmt.setInt(4, quantity);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error restoring stock for product ID: " + productId, e);
        }
    }

    private Product mapRowToProduct(ResultSet rs) throws SQLException {
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        return new Product(
                rs.getLong("id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                rs.getString("category"),
                rs.getBoolean("is_active"),
                createdTs != null ? createdTs.toInstant() : Instant.now(),
                updatedTs != null ? updatedTs.toInstant() : Instant.now()
        );
    }
}
