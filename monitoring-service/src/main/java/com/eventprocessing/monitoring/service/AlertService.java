package com.eventprocessing.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Service for sending alerts via SNS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final SnsClient snsClient;

    @Value("${aws.sns.alert-topic-arn}")
    private String alertTopicArn;

    /**
     * Send alert notification
     */
    public void sendAlert(String subject, String message) {
        log.warn("Sending alert: {} - {}", subject, message);

        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(alertTopicArn)
                    .subject(subject)
                    .message(message)
                    .build();

            snsClient.publish(request);
            log.info("Alert sent successfully");

        } catch (Exception e) {
            log.error("Error sending alert", e);
        }
    }
}
