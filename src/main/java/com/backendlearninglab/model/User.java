package com.backendlearninglab.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a user in the system.
 * <p>
 * WHY WRITE THIS MANUALLY INSTEAD OF USING LOMBOK?
 * 1. Lombok annotations like @Data generate toString, equals, hashCode, getters, and setters
 *    invisibly. Beginners never learn that mutable entities in collections need stable hashCode,
 *    or how circular references in toString() trigger StackOverflowError.
 * 2. Manual implementation allows domain invariants and validation right at object construction.
 */
public class User {

    private Long id;
    private String email;
    private String passwordHash;
    private String fullName;
    private UserRole role;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    // Default constructor (required by many mapping frameworks and reflective tools)
    public User() {
    }

    // Full constructor for constructing populated entities from database rows
    public User(Long id, String email, String passwordHash, String fullName,
                UserRole role, String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        setEmail(email);
        setPasswordHash(passwordHash);
        setFullName(fullName);
        this.role = (role != null) ? role : UserRole.CUSTOMER;
        this.status = (status != null && !status.isBlank()) ? status : "ACTIVE";
        this.createdAt = (createdAt != null) ? createdAt : Instant.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : Instant.now();
    }

    // Convenience constructor for creating a new user prior to persistence
    public User(String email, String passwordHash, String fullName, UserRole role) {
        this(null, email, passwordHash, fullName, role, "ACTIVE", Instant.now(), Instant.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("User email must be non-null and contain '@': " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        this.fullName = fullName.trim();
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = (role != null) ? role : UserRole.CUSTOMER;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * WHY OVERRIDE equals AND hashCode?
     * By default, Object.equals uses reference identity (==).
     * In a backend database entity, two Java instances representing the same database row
     * (same ID) must be considered equal.
     * Note: If ID is null (entity not yet persisted), we fall back to business key (email).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        if (id != null && user.id != null) {
            return Objects.equals(id, user.id);
        }
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(email);
    }

    @Override
    public String toString() {
        // SECURITY NOTE: Never include passwordHash in toString() to avoid credential leaks in logs!
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
