package com.backendlearninglab.repository;

import com.backendlearninglab.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User domain operations.
 * <p>
 * WHY DEFINE AN INTERFACE?
 * The Repository Pattern decouples business logic from data access mechanisms.
 * Tomorrow, we can swap pure JDBC for Spring Data JPA without modifying
 * a single line of business code in services!
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    boolean updateStatus(Long userId, String newStatus);

    boolean deleteById(Long id);
}
