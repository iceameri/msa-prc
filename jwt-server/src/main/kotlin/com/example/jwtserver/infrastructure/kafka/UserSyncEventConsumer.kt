package com.example.jwtserver.infrastructure.kafka

import com.example.jwtserver.application.service.IdempotentEventGuard
import com.example.jwtserver.application.service.UserSyncService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Instant

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
            // updatedAt 없는 구버전 메시지는 Instant.EPOCH로 처리:
            // DB에 이미 실제 시각이 저장되어 있으면 WHERE 조건에서 걸러지고,
            // 레코드가 없으면 INSERT로 진행되므로 안전
            val updatedAt = payload["updatedAt"]?.toString()?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?: Instant.EPOCH

            idempotentEventGuard.runIfNew(eventId, record.topic()) {
                userSyncService.sync(userId, username, updatedAt)
            }
        } catch (ex: Exception) {
            log.error("Invalid user-sync message format", ex)
        }
    }
}
