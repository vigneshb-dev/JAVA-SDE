package com.backendlearninglab.spring.controller;

import com.backendlearninglab.spring.dto.request.CreateOrderRequest;
import com.backendlearninglab.spring.dto.request.UpdateOrderStatusRequest;
import com.backendlearninglab.spring.dto.response.ApiResponse;
import com.backendlearninglab.spring.dto.response.OrderResponse;
import com.backendlearninglab.spring.service.OrderJpaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderJpaService orderService;

    public OrderController(OrderJpaService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/v1/orders
     * Executes atomic multi-table checkout transaction.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ApiResponse.ok("Order placed successfully", response);
    }

    /**
     * POST /api/v1/orders/deliberate-failure
     * Used for student testing: Verifies that an exception rolls back inventory and order tables!
     */
    @PostMapping("/deliberate-failure")
    public ApiResponse<String> testTransactionRollback(@Valid @RequestBody CreateOrderRequest request) {
        orderService.placeOrderWithDeliberateFailure(request);
        return ApiResponse.ok("Should not reach here");
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getOrderById(id));
    }

    /**
     * GET /api/v1/orders
     * GET /api/v1/orders?userId=1
     */
    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(@RequestParam(required = false) Long userId) {
        List<OrderResponse> orders = (userId != null)
                ? orderService.getOrdersByUserId(userId)
                : orderService.getAllOrders();
        return ApiResponse.ok(orders);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response = orderService.updateOrderStatus(id, request.status());
        return ApiResponse.ok("Order status updated", response);
    }
}
