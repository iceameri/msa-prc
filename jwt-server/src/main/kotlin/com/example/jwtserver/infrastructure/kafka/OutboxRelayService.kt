package com.example.jwtserver.infrastructure.kafka

import com.example.jwtserver.application.port.out.OutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class OutboxRelayService(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(OutboxRelayService::class.java)

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

    // claim 후 크래시된 이벤트를 주기적으로 복구
    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    fun cleanupStaleClaims() {
        outboxRepository.resetStaleClaims()
    }

    // 전송 완료된 오래된 이벤트 정리
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    fun cleanupProcessed() {
        outboxRepository.deleteProcessed()
    }

    private fun topicFor(eventType: String) = when (eventType) {
        "REPORT_CREATED" -> "reports"
        "POST_CREATED"   -> "outbox.events"
        else             -> "notifications"
    }
}
