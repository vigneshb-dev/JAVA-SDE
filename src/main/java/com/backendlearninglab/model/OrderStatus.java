package com.backendlearninglab.model;

/**
 * State machine representing order lifecycle stages.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public static OrderStatus fromString(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return PENDING;
        }
        try {
            return OrderStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown OrderStatus: " + statusStr);
        }
    }
}
