package com.example.jwtserver.application.service

import com.example.jwtserver.domain.event.ProcessedEventRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IdempotentEventGuard(private val processedEventRepository: ProcessedEventRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun runIfNew(eventId: String, topic: String, block: () -> Unit) {
        val inserted = processedEventRepository.insertIfAbsent(eventId, topic)
        if (!inserted) {
            log.debug("Skipping duplicate event: {}", eventId)
            return
        }
        block()
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun cleanup() {
        processedEventRepository.deleteOlderThan(30)
    }
}
