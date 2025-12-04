package com.eventprocessing.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Model for failed events (Dead Letter Queue)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedEvent {

    private String eventId;

    private String eventType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant originalTimestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private Instant failedAt = Instant.now();

    private String failureReason;

    private String stackTrace;

    private Integer totalRetries;

    private Event originalEvent;

    private String serviceName;
}
