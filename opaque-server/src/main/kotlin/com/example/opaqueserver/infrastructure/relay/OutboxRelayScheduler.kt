package com.example.opaqueserver.infrastructure.relay

import com.example.opaqueserver.domain.outbox.OutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OutboxRelayScheduler(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(OutboxRelayScheduler::class.java)

    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    fun relay() {
        val events = outboxRepository.findAndClaim(limit = 100)
        if (events.isEmpty()) return

        for (event in events) {
            val id = event.id ?: run {
                log.error("Claimed outbox event has null id — stale cleanup will recover it")
                return
            }
            try {
                kafkaTemplate.send(event.topic, event.aggregateKey, event.payload)
                    .get(5, TimeUnit.SECONDS)
                outboxRepository.delete(id)
            } catch (ex: InterruptedException) {
                outboxRepository.unclaim(id)
                Thread.currentThread().interrupt()
                return
            } catch (ex: Exception) {
                outboxRepository.unclaim(id)
                log.warn("Failed to relay outbox event id={} topic={}: {}", id, event.topic, ex.message)
                return
            }
        }
    }

    // claim 후 크래시된 이벤트를 주기적으로 복구
    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    fun cleanupStaleClaims() {
        outboxRepository.resetStaleClaims()
    }
}
