package com.eventprocessing.consumer.listener;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.util.JsonUtil;
import com.eventprocessing.consumer.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static com.eventprocessing.common.constants.AppConstants.KAFKA_TOPIC_EVENTS;
import static com.eventprocessing.common.constants.AppConstants.KAFKA_TOPIC_HIGH_PRIORITY;

/**
 * Kafka listener for consuming events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {

    private final EventProcessingService eventProcessingService;

    /**
     * Listen to standard events topic
     */
    @KafkaListener(topics = KAFKA_TOPIC_EVENTS, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.debug("Consumed event from Kafka: key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        try {
            Event event = JsonUtil.fromJson(record.value(), Event.class);
            eventProcessingService.processEvent(event);

            // Manually acknowledge after successful processing
            acknowledgment.acknowledge();
            log.debug("Acknowledged event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Error consuming event from Kafka: {}", record.key(), e);
            // Don't acknowledge - message will be redelivered
            // For production, consider implementing exponential backoff
        }
    }

    /**
     * Listen to high priority events topic
     */
    @KafkaListener(topics = KAFKA_TOPIC_HIGH_PRIORITY, groupId = "${spring.kafka.consumer.group-id}-high-priority", containerFactory = "kafkaListenerContainerFactory")
    public void consumeHighPriorityEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("Consumed HIGH PRIORITY event from Kafka: key={}", record.key());

        try {
            Event event = JsonUtil.fromJson(record.value(), Event.class);
            // Process high priority events immediately
            eventProcessingService.processEvent(event);

            acknowledgment.acknowledge();
            log.info("Acknowledged high priority event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Error consuming high priority event from Kafka: {}", record.key(), e);
        }
    }
}
