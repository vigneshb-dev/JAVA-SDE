package com.backendlearninglab.repository;

import com.backendlearninglab.exception.DatabaseOperationException;
import com.backendlearninglab.model.Order;
import com.backendlearninglab.model.OrderItem;
import com.backendlearninglab.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
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
 * Pure JDBC implementation of {@link OrderRepository}.
 */
public class JdbcOrderRepository implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcOrderRepository.class);
    private final DataSource dataSource;

    public JdbcOrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Order saveInTransaction(Order order, Connection conn) {
        String sql = "INSERT INTO orders (order_number, user_id, status, total_amount, shipping_address, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Instant now = Instant.now();
            pstmt.setString(1, order.getOrderNumber());
            pstmt.setLong(2, order.getUserId());
            pstmt.setString(3, order.getStatus().name());
            pstmt.setBigDecimal(4, order.getTotalAmount());
            pstmt.setString(5, order.getShippingAddress());
            pstmt.setTimestamp(6, Timestamp.from(now));
            pstmt.setTimestamp(7, Timestamp.from(now));

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setId(keys.getLong(1));
                    order.setCreatedAt(now);
                    order.setUpdatedAt(now);
                } else {
                    throw new DatabaseOperationException("Failed to generate order ID");
                }
            }
            return order;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving order: " + order.getOrderNumber(), e);
        }
    }

    @Override
    public OrderItem saveItemInTransaction(OrderItem item, Connection conn) {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, item.getOrderId());
            pstmt.setLong(2, item.getProductId());
            pstmt.setInt(3, item.getQuantity());
            pstmt.setBigDecimal(4, item.getUnitPrice());
            pstmt.setBigDecimal(5, item.getSubtotal());

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getLong(1));
                }
            }
            return item;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error inserting order item for order ID: " + item.getOrderId(), e);
        }
    }

    @Override
    public Optional<Order> findById(Long id) {
        String sql = "SELECT id, order_number, user_id, status, total_amount, shipping_address, created_at, updated_at " +
                     "FROM orders WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapRowToOrder(rs);
                    order.addItems(findItemsByOrderId(order.getId()));
                    return Optional.of(order);
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying order by ID: " + id, e);
        }
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        String sql = "SELECT id, order_number, user_id, status, total_amount, shipping_address, created_at, updated_at " +
                     "FROM orders WHERE order_number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, orderNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapRowToOrder(rs);
                    order.addItems(findItemsByOrderId(order.getId()));
                    return Optional.of(order);
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying order by number: " + orderNumber, e);
        }
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        String sql = "SELECT id, order_number, user_id, status, total_amount, shipping_address, created_at, updated_at " +
                     "FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRowToOrder(rs));
                }
            }
            return orders;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying orders for user ID: " + userId, e);
        }
    }

    @Override
    public List<OrderItem> findItemsByOrderId(Long orderId) {
        String sql = "SELECT id, order_id, product_id, quantity, unit_price, subtotal " +
                     "FROM order_items WHERE order_id = ? ORDER BY id";
        List<OrderItem> items = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem(
                            rs.getLong("id"),
                            rs.getLong("order_id"),
                            rs.getLong("product_id"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price")
                    );
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    items.add(item);
                }
            }
            return items;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error querying items for order ID: " + orderId, e);
        }
    }

    @Override
    public boolean updateStatus(Long orderId, OrderStatus newStatus) {
        String sql = "UPDATE orders SET status = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.name());
            pstmt.setTimestamp(2, Timestamp.from(Instant.now()));
            pstmt.setLong(3, orderId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating status for order ID: " + orderId, e);
        }
    }

    private Order mapRowToOrder(ResultSet rs) throws SQLException {
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        return new Order(
                rs.getLong("id"),
                rs.getString("order_number"),
                rs.getLong("user_id"),
                OrderStatus.fromString(rs.getString("status")),
                rs.getBigDecimal("total_amount"),
                rs.getString("shipping_address"),
                createdTs != null ? createdTs.toInstant() : Instant.now(),
                updatedTs != null ? updatedTs.toInstant() : Instant.now()
        );
    }
}
