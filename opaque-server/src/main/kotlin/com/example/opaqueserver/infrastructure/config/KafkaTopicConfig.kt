package com.example.opaqueserver.infrastructure.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    @Bean
    fun paymentSagaTopic(): NewTopic = TopicBuilder.name("payment.saga")
        .partitions(3)
        .replicas(1)
        .build()

    @Bean
    fun userManagementTopic(): NewTopic = TopicBuilder.name("user-management")
        .partitions(3)
        .replicas(1)
        .build()

    @Bean
    fun reportActionsTopic(): NewTopic = TopicBuilder.name("report-actions")
        .partitions(3)
        .replicas(1)
        .build()

    @Bean
    fun userActiveTopic(): NewTopic = TopicBuilder.name("user-active")
        .partitions(3)
        .replicas(1)
        .build()
}
