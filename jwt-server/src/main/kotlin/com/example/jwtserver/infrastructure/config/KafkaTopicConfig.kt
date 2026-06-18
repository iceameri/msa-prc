package com.example.jwtserver.infrastructure.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    @Bean
    fun notificationsTopic(): NewTopic = TopicBuilder.name("notifications")
        .partitions(3)
        .replicas(1)
        .build()

    @Bean
    fun reportsTopic(): NewTopic = TopicBuilder.name("reports")
        .partitions(3)
        .replicas(1)
        .build()

    @Bean
    fun outboxEventsTopic(): NewTopic = TopicBuilder.name("outbox.events")
        .partitions(3)
        .replicas(1)
        .build()
}
