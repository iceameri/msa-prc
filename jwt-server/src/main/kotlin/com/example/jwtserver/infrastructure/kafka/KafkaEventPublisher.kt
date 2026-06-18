package com.example.jwtserver.infrastructure.kafka

import com.example.jwtserver.application.port.out.EventPublishPort
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaEventPublisher(private val kafkaTemplate: KafkaTemplate<String, String>) : EventPublishPort {

    private val log = LoggerFactory.getLogger(KafkaEventPublisher::class.java)

    override fun publish(topic: String, key: String, payload: String) {
        kafkaTemplate.send(topic, key, payload)
            .whenComplete { _, ex ->
                if (ex != null) log.warn("Failed to publish to topic=$topic key=$key: ${ex.message}")
            }
    }
}
