package com.backendlearninglab.exception;

/**
 * Thrown when an order attempts to reserve or purchase more stock than is currently available.
 */
public class InsufficientStockException extends OrderFlowException {

    private final Long productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long productId, int requested, int available) {
        super(String.format("Insufficient stock for product ID %d: requested %d, but only %d available",
                productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
