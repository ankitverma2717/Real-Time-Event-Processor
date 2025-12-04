package com.eventprocessing.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for publishing metrics to CloudWatch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudWatchMetricsService {

    private final CloudWatchClient cloudWatchClient;

    @Value("${aws.cloudwatch.namespace}")
    private String namespace;

    /**
     * Publish metric to CloudWatch
     */
    public void publishMetric(String metricName, double value, StandardUnit unit) {
        try {
            MetricDatum datum = MetricDatum.builder()
                    .metricName(metricName)
                    .value(value)
                    .unit(unit)
                    .timestamp(Instant.now())
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(namespace)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
            log.debug("Published metric: {} = {}", metricName, value);

        } catch (Exception e) {
            log.error("Error publishing metric to CloudWatch", e);
        }
    }

    /**
     * Publish batch metrics
     */
    public void publishBatchMetrics(List<MetricDatum> metrics) {
        try {
            // CloudWatch allows max 20 metrics per request
            int batchSize = 20;
            for (int i = 0; i < metrics.size(); i += batchSize) {
                List<MetricDatum> batch = metrics.subList(i, Math.min(i + batchSize, metrics.size()));

                PutMetricDataRequest request = PutMetricDataRequest.builder()
                        .namespace(namespace)
                        .metricData(batch)
                        .build();

                cloudWatchClient.putMetricData(request);
            }

            log.info("Published {} metrics to CloudWatch", metrics.size());

        } catch (Exception e) {
            log.error("Error publishing batch metrics to CloudWatch", e);
        }
    }

    /**
     * Create custom metric for event throughput
     */
    public void publishThroughputMetric(long eventsProcessed, long timeWindowSeconds) {
        double throughput = (double) eventsProcessed / timeWindowSeconds;
        publishMetric("EventThroughput", throughput, StandardUnit.COUNT_SECOND);
    }

    /**
     * Create custom metric for latency
     */
    public void publishLatencyMetric(long latencyMs) {
        publishMetric("EventLatency", latencyMs, StandardUnit.MILLISECONDS);
    }

    /**
     * Create custom metric for error rate
     */
    public void publishErrorRateMetric(double errorRate) {
        publishMetric("ErrorRate", errorRate, StandardUnit.PERCENT);
    }
}
