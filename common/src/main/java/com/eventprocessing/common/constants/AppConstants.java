package com.eventprocessing.common.constants;

/**
 * Application-wide constants
 */
public class AppConstants {

    // Kafka Topics
    public static final String KAFKA_TOPIC_EVENTS = "events";
    public static final String KAFKA_TOPIC_HIGH_PRIORITY = "high-priority-events";
    public static final String KAFKA_TOPIC_DLQ = "events-dlq";

    // SQS Queue Names
    public static final String SQS_QUEUE_EVENTS = "event-processing-queue";
    public static final String SQS_QUEUE_DLQ = "event-dlq";
    public static final String SQS_QUEUE_HIGH_PRIORITY = "high-priority-queue";

    // SNS Topic Names
    public static final String SNS_TOPIC_NOTIFICATIONS = "event-notifications";
    public static final String SNS_TOPIC_ALERTS = "alert-notifications";

    // Retry Configuration
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long INITIAL_RETRY_DELAY_MS = 1000;
    public static final double RETRY_BACKOFF_MULTIPLIER = 2.0;

    // Performance Targets
    public static final int TARGET_THROUGHPUT_PER_SECOND = 100_000;
    public static final long TARGET_LATENCY_MS = 1000;

    // Batch Processing
    public static final int BATCH_SIZE = 100;
    public static final long BATCH_TIMEOUT_MS = 5000;

    // MongoDB Collections
    public static final String COLLECTION_EVENTS = "events";
    public static final String COLLECTION_FAILED_EVENTS = "failed_events";
    public static final String COLLECTION_METRICS = "metrics";
    public static final String COLLECTION_ALERTS = "alerts";

    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}
