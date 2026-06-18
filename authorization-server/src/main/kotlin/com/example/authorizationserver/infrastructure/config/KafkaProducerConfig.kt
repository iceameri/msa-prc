package com.example.authorizationserver.infrastructure.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class KafkaProducerConfig {

    @Bean
    fun kafkaTemplate(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String
    ): KafkaTemplate<String, String> {
        val props = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
        )
        return KafkaTemplate(DefaultKafkaProducerFactory(props))
    }

    @Bean
    fun userSyncTopic() = TopicBuilder.name("user-sync").partitions(3).replicas(1).build()

    @Bean
    fun usernameUpdatedTopic() = TopicBuilder.name("user.username.updated").partitions(3).replicas(1).build()
}
