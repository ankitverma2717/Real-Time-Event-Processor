package com.eventprocessing.dataingestion.service;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventStatus;
import com.eventprocessing.dataingestion.entity.EventEntity;
import com.eventprocessing.dataingestion.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for ingesting events into MongoDB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestionService {

    private final EventRepository eventRepository;

    /**
     * Ingest a single event
     */
    @Async
    public CompletableFuture<EventEntity> ingestEvent(Event event) {
        log.debug("Ingesting event: {}", event.getEventId());

        EventEntity entity = convertToEntity(event);
        EventEntity saved = eventRepository.save(entity);

        log.info("Successfully ingested event: {}", event.getEventId());
        return CompletableFuture.completedFuture(saved);
    }

    /**
     * Batch ingest events for performance
     */
    @Async
    public CompletableFuture<List<EventEntity>> batchIngestEvents(List<Event> events) {
        log.info("Batch ingesting {} events", events.size());

        List<EventEntity> entities = events.stream()
                .map(this::convertToEntity)
                .toList();

        List<EventEntity> saved = eventRepository.saveAll(entities);

        log.info("Successfully batch ingested {} events", events.size());
        return CompletableFuture.completedFuture(saved);
    }

    /**
     * Get event by ID
     */
    public EventEntity getEventById(String eventId) {
        return eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
    }

    /**
     * Get events by type
     */
    public List<EventEntity> getEventsByType(String eventType) {
        return eventRepository.findByEventType(eventType);
    }

    /**
     * Get events in time range
     */
    public List<EventEntity> getEventsInTimeRange(Instant startTime, Instant endTime) {
        return eventRepository.findEventsInTimeRange(startTime, endTime);
    }

    /**
     * Get events by status
     */
    public List<EventEntity> getEventsByStatus(EventStatus status) {
        return eventRepository.findByStatus(status);
    }

    /**
     * Get event count by status
     */
    public long getEventCountByStatus(EventStatus status) {
        return eventRepository.countByStatus(status);
    }

    /**
     * Convert Event to EventEntity
     */
    private EventEntity convertToEntity(Event event) {
        return EventEntity.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .timestamp(event.getTimestamp())
                .payload(event.getPayload())
                .status(event.getStatus())
                .retryCount(event.getRetryCount())
                .correlationId(event.getCorrelationId())
                .source(event.getSource())
                .metadata(event.getMetadata())
                .processedAt(event.getProcessedAt())
                .errorMessage(event.getErrorMessage())
                .createdAt(Instant.now())
                .build();
    }
}
