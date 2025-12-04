package com.eventprocessing.producer.controller;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.producer.service.EventPublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for event submission
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventPublisherService eventPublisherService;

    @Value("${aws.sqs.queue-url}")
    private String sqsQueueUrl;

    @Value("${aws.sqs.high-priority-queue-url}")
    private String highPrioritySqsQueueUrl;

    @Value("${aws.sns.topic-arn}")
    private String snsTopicArn;

    /**
     * Submit a new event
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitEvent(@Valid @RequestBody Event event) {
        log.info("Received event submission: {}", event.getEventId());

        try {
            // Publish to Kafka (asynchronous)
            CompletableFuture<Void> kafkaFuture = eventPublisherService.publishToKafka(event)
                    .thenAccept(result -> log.info("Event {} published to Kafka", event.getEventId()));

            // Send to SQS (synchronous for reliability)
            String queueUrl = isHighPriority(event) ? highPrioritySqsQueueUrl : sqsQueueUrl;
            eventPublisherService.sendToSqs(event, queueUrl);

            // Publish to SNS for notifications (async)
            CompletableFuture.runAsync(() -> eventPublisherService.publishToSns(event, snsTopicArn));

            Map<String, Object> response = new HashMap<>();
            response.put("eventId", event.getEventId());
            response.put("status", "ACCEPTED");
            response.put("message", "Event submitted successfully");
            response.put("timestamp", event.getTimestamp());

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            log.error("Error submitting event {}", event.getEventId(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("eventId", event.getEventId());
            errorResponse.put("status", "FAILED");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Submit multiple events in batch
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> submitBatchEvents(@RequestBody java.util.List<@Valid Event> events) {
        log.info("Received batch submission of {} events", events.size());

        int successCount = 0;
        int failureCount = 0;

        for (Event event : events) {
            try {
                eventPublisherService.publishToKafka(event);
                String queueUrl = isHighPriority(event) ? highPrioritySqsQueueUrl : sqsQueueUrl;
                eventPublisherService.sendToSqs(event, queueUrl);
                successCount++;
            } catch (Exception e) {
                log.error("Error submitting event {} in batch", event.getEventId(), e);
                failureCount++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalEvents", events.size());
        response.put("successCount", successCount);
        response.put("failureCount", failureCount);
        response.put("status", failureCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS");

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "event-producer");
        return ResponseEntity.ok(response);
    }

    /**
     * Check if event is high priority
     */
    private boolean isHighPriority(Event event) {
        return event.getMetadata() != null &&
                "high".equalsIgnoreCase(event.getMetadata().get("priority"));
    }
}
