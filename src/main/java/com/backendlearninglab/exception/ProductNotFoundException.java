package com.backendlearninglab.exception;

/**
 * Thrown when an operation targets a product ID or SKU that does not exist in the database.
 */
public class ProductNotFoundException extends OrderFlowException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product not found with id: " + productId);
        this.productId = productId;
    }

    public ProductNotFoundException(String message) {
        super(message);
        this.productId = null;
    }

    public Long getProductId() {
        return productId;
    }
}
