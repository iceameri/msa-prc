package com.example.opaqueserver.infrastructure.kafka

import com.example.opaqueserver.application.port.out.EventPublishPort
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaEventPublisher(private val kafkaTemplate: KafkaTemplate<String, String>) : EventPublishPort {

    private val log = LoggerFactory.getLogger(KafkaEventPublisher::class.java)

    override fun publish(topic: String, key: String, payload: String) {
        val whenComplete = kafkaTemplate.send(topic, key, payload)
            .whenComplete { _, ex ->
                if (ex != null) log.warn("Failed to publish topic={} key={}: {}", topic, key, ex.message)
            }
    }
}
