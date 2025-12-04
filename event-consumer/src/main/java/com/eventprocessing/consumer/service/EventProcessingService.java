package com.eventprocessing.consumer.service;

import com.eventprocessing.common.constants.AppConstants;
import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventStatus;
import com.eventprocessing.common.util.JsonUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Core service for processing events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final DeadLetterQueueService dlqService;

    /**
     * Process an event with circuit breaker and retry logic
     */
    @CircuitBreaker(name = "eventProcessing", fallbackMethod = "processFallback")
    @Retry(name = "eventProcessing")
    public void processEvent(Event event) {
        log.info("Processing event: {} of type: {}", event.getEventId(), event.getEventType());

        try {
            // Mark as processing
            event.markAsProcessing();

            // Simulate event processing logic
            performBusinessLogic(event);

            // Mark as completed
            event.markAsCompleted();
            log.info("Successfully processed event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Error processing event: {}", event.getEventId(), e);
            handleProcessingFailure(event, e);
            throw e; // Re-throw for retry mechanism
        }
    }

    /**
     * Business logic for event processing
     */
    private void performBusinessLogic(Event event) {
        // Simulate different processing based on event type
        switch (event.getEventType()) {
            case "user.created":
                processUserCreated(event);
                break;
            case "order.placed":
                processOrderPlaced(event);
                break;
            case "payment.completed":
                processPaymentCompleted(event);
                break;
            default:
                processGenericEvent(event);
        }
    }

    private void processUserCreated(Event event) {
        log.info("Processing user created event: {}", event.getEventId());
        // Add user-specific processing logic here
        simulateProcessingDelay(50);
    }

    private void processOrderPlaced(Event event) {
        log.info("Processing order placed event: {}", event.getEventId());
        // Add order-specific processing logic here
        simulateProcessingDelay(100);
    }

    private void processPaymentCompleted(Event event) {
        log.info("Processing payment completed event: {}", event.getEventId());
        // Add payment-specific processing logic here
        simulateProcessingDelay(75);
    }

    private void processGenericEvent(Event event) {
        log.info("Processing generic event: {}", event.getEventId());
        // Add generic processing logic here
        simulateProcessingDelay(30);
    }

    /**
     * Simulate processing delay
     */
    private void simulateProcessingDelay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
    }

    /**
     * Handle processing failure
     */
    private void handleProcessingFailure(Event event, Exception e) {
        event.incrementRetryCount();

        // If max retries exceeded, send to DLQ
        if (event.getRetryCount() >= AppConstants.MAX_RETRY_ATTEMPTS) {
            log.error("Max retries exceeded for event: {}. Sending to DLQ", event.getEventId());
            event.markAsFailed(e.getMessage());
            dlqService.sendToDeadLetterQueue(event, e);
        } else {
            log.warn("Retry attempt {} for event: {}", event.getRetryCount(), event.getEventId());
        }
    }

    /**
     * Fallback method when circuit breaker opens
     */
    private void processFallback(Event event, Exception e) {
        log.error("Circuit breaker opened. Sending event {} directly to DLQ", event.getEventId());
        event.markAsFailed("Circuit breaker open: " + e.getMessage());
        dlqService.sendToDeadLetterQueue(event, e);
    }
}
