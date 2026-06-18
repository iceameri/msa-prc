package com.example.jwtserver.infrastructure.kafka

import com.example.jwtserver.application.port.out.OutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class OutboxRelayService(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(OutboxRelayService::class.java)

    @Scheduled(fixedDelay = 5000)
    fun relay() {
        val events = outboxRepository.findUnpublished(100)
        events.forEach { event ->
            try {
                kafkaTemplate.send(topicFor(event.eventType), event.aggregateId, event.payload).get()
                outboxRepository.markPublished(event.id!!)
            } catch (ex: Exception) {
                log.warn("Failed to relay outbox event id=${event.id} type=${event.eventType}: ${ex.message}")
            }
        }
    }

    private fun topicFor(eventType: String) = when (eventType) {
        "REPORT_CREATED" -> "reports"
        "POST_CREATED"   -> "outbox.events"
        else             -> "notifications"
    }
}
