package com.eventprocessing.producer.service;

import com.eventprocessing.common.constants.AppConstants;
import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing events to Kafka and SQS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SqsClient sqsClient;
    private final SnsClient snsClient;

    /**
     * Publish event to Kafka
     */
    public CompletableFuture<SendResult<String, String>> publishToKafka(Event event) {
        String topic = determineKafkaTopic(event);
        String eventJson = JsonUtil.toJson(event);

        log.info("Publishing event {} to Kafka topic: {}", event.getEventId(), topic);

        return kafkaTemplate.send(topic, event.getEventId(), eventJson)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} to Kafka", event.getEventId(), ex);
                    } else {
                        log.info("Successfully published event {} to Kafka partition {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }

    /**
     * Send event to SQS
     */
    public void sendToSqs(Event event, String queueUrl) {
        String eventJson = JsonUtil.toJson(event);

        log.info("Sending event {} to SQS queue: {}", event.getEventId(), queueUrl);

        try {
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(eventJson)
                    .messageGroupId(event.getEventType()) // For FIFO queues
                    .messageDeduplicationId(event.getEventId())
                    .build();

            sqsClient.sendMessage(request);
            log.info("Successfully sent event {} to SQS", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to send event {} to SQS", event.getEventId(), e);
            throw new RuntimeException("Failed to send event to SQS", e);
        }
    }

    /**
     * Publish notification to SNS
     */
    public void publishToSns(Event event, String topicArn) {
        String eventJson = JsonUtil.toJson(event);

        log.info("Publishing event {} to SNS topic: {}", event.getEventId(), topicArn);

        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(eventJson)
                    .subject("Event Notification: " + event.getEventType())
                    .build();

            snsClient.publish(request);
            log.info("Successfully published event {} to SNS", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event {} to SNS", event.getEventId(), e);
            throw new RuntimeException("Failed to publish event to SNS", e);
        }
    }

    /**
     * Determine which Kafka topic to use based on event type
     */
    private String determineKafkaTopic(Event event) {
        // High priority events go to separate topic
        if (event.getMetadata() != null &&
                "high".equalsIgnoreCase(event.getMetadata().get("priority"))) {
            return AppConstants.KAFKA_TOPIC_HIGH_PRIORITY;
        }
        return AppConstants.KAFKA_TOPIC_EVENTS;
    }
}
