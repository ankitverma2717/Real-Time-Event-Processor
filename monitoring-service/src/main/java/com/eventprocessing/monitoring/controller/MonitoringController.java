package com.eventprocessing.monitoring.controller;

import com.eventprocessing.monitoring.service.CloudWatchMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for monitoring and metrics
 */
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final CloudWatchMetricsService cloudWatchMetricsService;

    /**
     * Publish custom metric
     */
    @PostMapping("/metrics")
    public ResponseEntity<Map<String, String>> publishMetric(
            @RequestParam String metricName,
            @RequestParam double value,
            @RequestParam(defaultValue = "COUNT") String unit) {

        StandardUnit standardUnit = StandardUnit.fromValue(unit);
        cloudWatchMetricsService.publishMetric(metricName, value, standardUnit);

        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Metric published successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "monitoring-service");
        return ResponseEntity.ok(response);
    }
}
