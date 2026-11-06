package com.rideflow.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Creates the topics on startup if they don't exist yet (idempotent, safe in every service). */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic rideEventsTopic() {
        return TopicBuilder.name(Topics.RIDE_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic driverAssignmentsTopic() {
        return TopicBuilder.name(Topics.DRIVER_ASSIGNMENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic driverLocationsTopic() {
        return TopicBuilder.name(Topics.DRIVER_LOCATIONS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(Topics.USER_EVENTS).partitions(1).replicas(1).build();
    }
}
