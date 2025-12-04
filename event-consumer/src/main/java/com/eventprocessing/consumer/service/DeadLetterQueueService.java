package com.eventprocessing.consumer.service;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.FailedEvent;
import com.eventprocessing.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.eventprocessing.common.constants.AppConstants.KAFKA_TOPIC_DLQ;

/**
 * Service for handling failed events (Dead Letter Queue)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SqsClient sqsClient;

    @Value("${aws.sqs.dlq-url}")
    private String dlqUrl;

    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * Send failed event to dead letter queue
     */
    public void sendToDeadLetterQueue(Event event, Exception exception) {
        log.error("Sending event {} to Dead Letter Queue", event.getEventId());

        FailedEvent failedEvent = FailedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .originalTimestamp(event.getTimestamp())
                .failureReason(exception.getMessage())
                .stackTrace(getStackTrace(exception))
                .totalRetries(event.getRetryCount())
                .originalEvent(event)
                .serviceName(serviceName)
                .build();

        // Send to Kafka DLQ topic
        sendToKafkaDlq(failedEvent);

        // Send to SQS DLQ
        sendToSqsDlq(failedEvent);
    }

    /**
     * Send to Kafka DLQ topic
     */
    private void sendToKafkaDlq(FailedEvent failedEvent) {
        try {
            String failedEventJson = JsonUtil.toJson(failedEvent);
            kafkaTemplate.send(KAFKA_TOPIC_DLQ, failedEvent.getEventId(), failedEventJson);
            log.info("Sent failed event {} to Kafka DLQ", failedEvent.getEventId());
        } catch (Exception e) {
            log.error("Failed to send event {} to Kafka DLQ", failedEvent.getEventId(), e);
        }
    }

    /**
     * Send to SQS DLQ
     */
    private void sendToSqsDlq(FailedEvent failedEvent) {
        try {
            String failedEventJson = JsonUtil.toJson(failedEvent);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .messageBody(failedEventJson)
                    .messageGroupId(failedEvent.getEventType())
                    .messageDeduplicationId(failedEvent.getEventId() + "-" + System.currentTimeMillis())
                    .build();

            sqsClient.sendMessage(request);
            log.info("Sent failed event {} to SQS DLQ", failedEvent.getEventId());
        } catch (Exception e) {
            log.error("Failed to send event {} to SQS DLQ", failedEvent.getEventId(), e);
        }
    }

    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}
