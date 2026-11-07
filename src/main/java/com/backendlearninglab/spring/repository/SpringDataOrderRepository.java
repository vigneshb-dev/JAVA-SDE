package com.backendlearninglab.spring.repository;

import com.backendlearninglab.spring.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByUserId(Long userId);

    // -------------------------------------------------------------------------
    // N+1 QUERY PROBLEM SOLUTIONS
    // -------------------------------------------------------------------------

    /**
     * SOLUTION 1: JPQL JOIN FETCH
     * <p>
     * WHAT HAPPENS INTERNALLY:
     * Tells Hibernate to generate an explicit SQL INNER/LEFT JOIN fetching the Order,
     * its line items, and its user in a SINGLE SQL network round-trip!
     * Eliminates the N+1 SELECT queries entirely.
     */
    @Query("SELECT DISTINCT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.items " +
           "LEFT JOIN FETCH o.user " +
           "WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithItemsAndUserJoinFetch(@Param("id") Long id);

    /**
     * SOLUTION 2: Spring Data @EntityGraph
     * <p>
     * WHAT HAPPENS INTERNALLY:
     * Declaratively overrides the FetchType.LAZY on specified attributes for this query only.
     * Hibernate dynamically adds the necessary SQL JOINs to fetch 'items' and 'user' eagerly.
     */
    @EntityGraph(attributePaths = {"items", "user"})
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithEntityGraph(@Param("id") Long id);

    @EntityGraph(attributePaths = {"items", "user"})
    List<OrderEntity> findAll();
}
