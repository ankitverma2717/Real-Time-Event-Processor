package com.eventprocessing.dataingestion.listener;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.util.JsonUtil;
import com.eventprocessing.dataingestion.service.EventIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static com.eventprocessing.common.constants.AppConstants.KAFKA_TOPIC_EVENTS;

/**
 * Kafka listener for ingesting events to MongoDB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataIngestionListener {

    private final EventIngestionService eventIngestionService;

    @KafkaListener(topics = KAFKA_TOPIC_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            Event event = JsonUtil.fromJson(record.value(), Event.class);
            log.debug("Ingesting event: {}", event.getEventId());

            eventIngestionService.ingestEvent(event);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error ingesting event from Kafka", e);
        }
    }
}
