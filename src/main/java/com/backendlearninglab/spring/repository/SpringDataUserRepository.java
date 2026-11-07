package com.backendlearninglab.spring.repository;

import com.backendlearninglab.spring.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for UserEntity.
 * <p>
 * WHAT SPRING DATA DOES FOR YOU:
 * At application startup, Spring Data generates a dynamic proxy implementing this interface.
 * You get 30+ production-tested CRUD and paging methods (save, findById, findAll, deleteById, count)
 * with ZERO lines of boilerplate SQL or JDBC mapping code!
 */
@Repository
public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
