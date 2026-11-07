package com.backendlearninglab.model;

/**
 * Represents the authorization role of a user in OrderFlow.
 * <p>
 * WHY ENUM:
 * Enums provide type-safety, preventing typos like "custmr" or "ADMINISTRATOR"
 * at compile-time instead of discovering them at runtime in production.
 */
public enum UserRole {
    USER,
    CUSTOMER,
    ADMIN,
    INVENTORY_MANAGER,
    MANAGER;

    public static UserRole fromString(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return USER;
        }
        String clean = roleStr.trim().toUpperCase();
        if ("CUSTOMER".equals(clean)) return USER;
        if ("MANAGER".equals(clean)) return INVENTORY_MANAGER;
        try {
            return UserRole.valueOf(clean);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: '" + roleStr + "'. Expected USER, ADMIN, or INVENTORY_MANAGER.");
        }
    }
}
