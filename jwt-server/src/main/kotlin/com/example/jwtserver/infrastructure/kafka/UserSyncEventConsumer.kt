package com.example.jwtserver.infrastructure.kafka

import com.example.jwtserver.application.service.IdempotentEventGuard
import com.example.jwtserver.application.service.UserSyncService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class UserSyncEventConsumer(
    private val userSyncService: UserSyncService,
    private val idempotentEventGuard: IdempotentEventGuard,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["user-sync"], groupId = "jwt-server-group")
    fun consume(record: ConsumerRecord<String, String>) {
        // topic:partition:offset 조합은 Kafka 내에서 전역 유일 — 중복 메시지 식별자로 사용
        val eventId = "${record.topic()}:${record.partition()}:${record.offset()}"
        // null 값(Kafka tombstone 레코드) 은 동기화 대상이 아님
        if (record.value() == null) {
            log.debug("Skipping tombstone record at {}", eventId)
            return
        }
        try {
            @Suppress("UNCHECKED_CAST")
            val payload = objectMapper.readValue(record.value(), Map::class.java) as Map<String, Any?>

            val userId = payload["userId"]?.toString()?.toLongOrNull() ?: run {
                log.error("Missing or invalid userId in user-sync message")
                return
            }
            val username = payload["username"]?.toString() ?: run {
                log.error("Missing username in user-sync message")
                return
            }
            val enabled = payload["enabled"]?.toString()?.toBooleanStrictOrNull() ?: true
            val status  = payload["status"]?.toString() ?: "ACTIVE"
            val version = payload["version"]?.toString()?.toLongOrNull() ?: 0L

            idempotentEventGuard.runIfNew(eventId, record.topic()) {
                userSyncService.sync(userId, username, enabled, status, version)
            }
        } catch (ex: Exception) {
            log.error("Invalid user-sync message format", ex)
        }
    }
}
