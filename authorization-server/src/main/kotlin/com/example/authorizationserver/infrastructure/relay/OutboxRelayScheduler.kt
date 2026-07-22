package com.example.authorizationserver.infrastructure.relay

import com.example.authorizationserver.infrastructure.persistence.OutboxJdbcRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OutboxRelayScheduler(
    private val outboxRepository: OutboxJdbcRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${outbox.relay.fixed-delay:1000}", initialDelay = 5000)
    fun relay() {
        val events = outboxRepository.findAndClaim(limit = 100)
        if (events.isEmpty()) return

        for (event in events) {
            val id = event.id ?: run {
                log.error("Claimed outbox event has null id — stale cleanup will recover it")
                return
            }
            try {
                kafkaTemplate.send(topicFor(event.eventType), event.aggregateId, event.payload)
                    .get(5, TimeUnit.SECONDS)
                outboxRepository.markSent(id)
            } catch (ex: InterruptedException) {
                outboxRepository.unclaim(id)
                Thread.currentThread().interrupt()
                return
            } catch (ex: Exception) {
                outboxRepository.unclaim(id)
                log.warn("Failed to relay outbox event id={} type={}: {}", id, event.eventType, ex.message)
                return
            }
        }
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    fun cleanupStaleClaims() { outboxRepository.resetStaleClaims() }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    fun cleanupProcessed() { outboxRepository.deleteProcessed() }

    private fun topicFor(eventType: String) = when (eventType) {
        "USER_SYNC"        -> "user-sync"
        "USERNAME_UPDATED" -> "user.username.updated"
        else               -> eventType.lowercase().replace("_", "-")
    }
}
