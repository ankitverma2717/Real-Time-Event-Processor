package com.eventprocessing.consumer.listener;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.util.JsonUtil;
import com.eventprocessing.consumer.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

/**
 * SQS poller for consuming events from SQS queues
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqsEventPoller {

    private final SqsClient sqsClient;
    private final EventProcessingService eventProcessingService;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    @Value("${aws.sqs.max-messages:10}")
    private Integer maxMessages;

    @Value("${aws.sqs.wait-time-seconds:20}")
    private Integer waitTimeSeconds;

    @Value("${aws.sqs.visibility-timeout:30}")
    private Integer visibilityTimeout;

    /**
     * Poll SQS queue for messages
     */
    @Scheduled(fixedDelay = 1000) // Poll every second
    public void pollMessages() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(maxMessages)
                    .waitTimeSeconds(waitTimeSeconds)
                    .visibilityTimeout(visibilityTimeout)
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = response.messages();

            if (!messages.isEmpty()) {
                log.info("Received {} messages from SQS", messages.size());

                for (Message message : messages) {
                    processMessage(message);
                }
            }

        } catch (Exception e) {
            log.error("Error polling SQS queue", e);
        }
    }

    /**
     * Process individual SQS message
     */
    private void processMessage(Message message) {
        try {
            Event event = JsonUtil.fromJson(message.body(), Event.class);
            log.info("Processing SQS message: {}", event.getEventId());

            // Process the event
            eventProcessingService.processEvent(event);

            // Delete message from queue after successful processing
            deleteMessage(message.receiptHandle());

        } catch (Exception e) {
            log.error("Error processing SQS message: {}", message.messageId(), e);
            // Message will become visible again after visibility timeout
            // Consider implementing exponential backoff for visibility timeout
        }
    }

    /**
     * Delete message from SQS queue
     */
    private void deleteMessage(String receiptHandle) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build();

            sqsClient.deleteMessage(deleteRequest);
            log.debug("Deleted message from SQS queue");

        } catch (Exception e) {
            log.error("Error deleting message from SQS", e);
        }
    }
}
