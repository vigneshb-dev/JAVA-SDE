package com.backendlearninglab.spring.repository;

import com.backendlearninglab.spring.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.ProcessedEventId> {

    boolean existsByIdEventIdAndIdConsumerName(String eventId, String consumerName);
}
