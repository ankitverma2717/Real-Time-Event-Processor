package com.eventprocessing.dataingestion.repository;

import com.eventprocessing.common.model.EventStatus;
import com.eventprocessing.dataingestion.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for events
 */
@Repository
public interface EventRepository extends MongoRepository<EventEntity, String> {

    Optional<EventEntity> findByEventId(String eventId);

    List<EventEntity> findByEventType(String eventType);

    List<EventEntity> findByStatus(EventStatus status);

    Page<EventEntity> findByEventTypeAndTimestampBetween(
            String eventType,
            Instant startTime,
            Instant endTime,
            Pageable pageable);

    List<EventEntity> findByStatusAndTimestampBefore(EventStatus status, Instant timestamp);

    @Query("{ 'timestamp': { $gte: ?0, $lt: ?1 } }")
    List<EventEntity> findEventsInTimeRange(Instant startTime, Instant endTime);

    @Query("{ 'eventType': ?0, 'status': ?1 }")
    List<EventEntity> findByEventTypeAndStatus(String eventType, EventStatus status);

    long countByStatus(EventStatus status);

    long countByEventTypeAndTimestampBetween(String eventType, Instant startTime, Instant endTime);
}
