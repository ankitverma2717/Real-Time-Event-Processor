package com.eventprocessing.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Core Event model representing an event in the system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @NotNull
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @NotNull
    private String eventType;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @NotNull
    private Map<String, Object> payload;

    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    @Builder.Default
    private Integer retryCount = 0;

    private String correlationId;

    private String source;

    private Map<String, String> metadata;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant processedAt;

    private String errorMessage;

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Mark event as processing
     */
    public void markAsProcessing() {
        this.status = EventStatus.PROCESSING;
    }

    /**
     * Mark event as completed
     */
    public void markAsCompleted() {
        this.status = EventStatus.COMPLETED;
        this.processedAt = Instant.now();
    }

    /**
     * Mark event as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = Instant.now();
    }
}
