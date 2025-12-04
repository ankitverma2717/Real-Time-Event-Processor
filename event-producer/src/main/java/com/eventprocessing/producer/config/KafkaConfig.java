package com.eventprocessing.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.eventprocessing.common.constants.AppConstants.*;

/**
 * Kafka configuration for topic creation
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(KAFKA_TOPIC_EVENTS)
                .partitions(6)
                .replicas(1)
                .compact()
                .build();
    }

    @Bean
    public NewTopic highPriorityTopic() {
        return TopicBuilder.name(KAFKA_TOPIC_HIGH_PRIORITY)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(KAFKA_TOPIC_DLQ)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
