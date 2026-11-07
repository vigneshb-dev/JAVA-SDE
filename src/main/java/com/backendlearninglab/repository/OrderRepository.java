package com.backendlearninglab.repository;

import com.backendlearninglab.model.Order;
import com.backendlearninglab.model.OrderItem;
import com.backendlearninglab.model.OrderStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order and OrderItem entities.
 */
public interface OrderRepository {

    /**
     * Inserts the order header within the provided transactional connection.
     */
    Order saveInTransaction(Order order, Connection conn);

    /**
     * Inserts an individual order line item within the provided transactional connection.
     */
    OrderItem saveItemInTransaction(OrderItem item, Connection conn);

    Optional<Order> findById(Long id);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    List<OrderItem> findItemsByOrderId(Long orderId);

    boolean updateStatus(Long orderId, OrderStatus newStatus);
}
