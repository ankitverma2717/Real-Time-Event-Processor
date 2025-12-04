package com.eventprocessing.monitoring.service;

import com.eventprocessing.common.model.EventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for monitoring system metrics and publishing to CloudWatch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectorService {

    private final MongoTemplate mongoTemplate;
    private final CloudWatchMetricsService cloudWatchMetricsService;
    private final AlertService alertService;

    @Value("${app.monitoring.alert.error-rate-threshold}")
    private double errorRateThreshold;

    private long previousEventCount = 0;

    /**
     * Collect and publish metrics every minute
     */
    @Scheduled(fixedDelayString = "${app.monitoring.interval}")
    public void collectAndPublishMetrics() {
        log.info("Collecting metrics...");

        try {
            Map<String, Long> metrics = collectMetrics();

            // Publish to CloudWatch
            publishMetrics(metrics);

            // Check for alerts
            checkAndTriggerAlerts(metrics);

        } catch (Exception e) {
            log.error("Error collecting metrics", e);
        }
    }

    /**
     * Collect metrics from MongoDB
     */
    private Map<String, Long> collectMetrics() {
        Map<String, Long> metrics = new HashMap<>();

        // Get total event count
        long totalEvents = mongoTemplate.count(
                new org.springframework.data.mongodb.core.query.Query(),
                "events");
        metrics.put("TotalEvents", totalEvents);

        // Get count by status
        long completedEvents = mongoTemplate.count(
                org.springframework.data.mongodb.core.query.Query.query(
                        Criteria.where("status").is(EventStatus.COMPLETED)),
                "events");
        metrics.put("CompletedEvents", completedEvents);

        long failedEvents = mongoTemplate.count(
                org.springframework.data.mongodb.core.query.Query.query(
                        Criteria.where("status").is(EventStatus.FAILED)),
                "events");
        metrics.put("FailedEvents", failedEvents);

        long processingEvents = mongoTemplate.count(
                org.springframework.data.mongodb.core.query.Query.query(
                        Criteria.where("status").is(EventStatus.PROCESSING)),
                "events");
        metrics.put("ProcessingEvents", processingEvents);

        // Get events in last minute
        Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
        long recentEvents = mongoTemplate.count(
                org.springframework.data.mongodb.core.query.Query.query(
                        Criteria.where("createdAt").gte(oneMinuteAgo)),
                "events");
        metrics.put("RecentEvents", recentEvents);

        return metrics;
    }

    /**
     * Publish metrics to CloudWatch
     */
    private void publishMetrics(Map<String, Long> metrics) {
        metrics.forEach(
                (key, value) -> cloudWatchMetricsService.publishMetric(key, value.doubleValue(), StandardUnit.COUNT));

        // Calculate and publish throughput
        long currentTotal = metrics.get("TotalEvents");
        long eventsProcessed = currentTotal - previousEventCount;
        previousEventCount = currentTotal;

        cloudWatchMetricsService.publishThroughputMetric(eventsProcessed, 60);

        // Calculate and publish error rate
        long completed = metrics.get("CompletedEvents");
        long failed = metrics.get("FailedEvents");
        long total = completed + failed;

        if (total > 0) {
            double errorRate = ((double) failed / total) * 100;
            cloudWatchMetricsService.publishErrorRateMetric(errorRate);
        }
    }

    /**
     * Check metrics and trigger alerts if thresholds exceeded
     */
    private void checkAndTriggerAlerts(Map<String, Long> metrics) {
        long completed = metrics.get("CompletedEvents");
        long failed = metrics.get("FailedEvents");
        long total = completed + failed;

        if (total > 0) {
            double errorRate = ((double) failed / total) * 100;

            if (errorRate > errorRateThreshold) {
                alertService.sendAlert(
                        "High Error Rate",
                        String.format("Error rate is %.2f%%, exceeding threshold of %.2f%%",
                                errorRate, errorRateThreshold));
            }
        }

        // Check for stale processing events
        long processingEvents = metrics.get("ProcessingEvents");
        if (processingEvents > 100) {
            alertService.sendAlert(
                    "High Processing Queue",
                    String.format("%d events stuck in PROCESSING state", processingEvents));
        }
    }
}
