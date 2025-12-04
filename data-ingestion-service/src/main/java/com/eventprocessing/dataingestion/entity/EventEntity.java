package com.eventprocessing.dataingestion.entity;

import com.eventprocessing.common.model.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB entity for storing events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "events")
@CompoundIndexes({
        @CompoundIndex(name = "eventType_timestamp_idx", def = "{'eventType': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "status_timestamp_idx", def = "{'status': 1, 'timestamp': -1}")
})
public class EventEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    @Indexed
    private String eventType;

    @Indexed
    private Instant timestamp;

    private Map<String, Object> payload;

    @Indexed
    private EventStatus status;

    private Integer retryCount;

    private String correlationId;

    private String source;

    private Map<String, String> metadata;

    private Instant processedAt;

    private String errorMessage;

    @Indexed
    private Instant createdAt;
}
