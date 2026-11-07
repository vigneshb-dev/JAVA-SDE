package com.backendlearninglab.spring.repository;

import com.backendlearninglab.spring.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataOutboxRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxEventEntity.OutboxStatus status, Pageable pageable);
}
