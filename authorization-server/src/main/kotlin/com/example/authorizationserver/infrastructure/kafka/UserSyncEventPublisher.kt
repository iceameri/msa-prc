package com.example.authorizationserver.infrastructure.kafka

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Suppress("MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_WARNING")
@Component
class UserSyncEventPublisher(private val kafkaTemplate: KafkaTemplate<String, String>) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(userId: Long, username: String) {
        // updatedAt: Consumer 측에서 순서 역전 감지에 사용 — 발행 직전 시각을 기록
        val updatedAt = Instant.now()
        kafkaTemplate.send(
            "user-sync", userId.toString(),
            """{"userId":$userId,"username":"$username","updatedAt":"$updatedAt"}"""
        ).whenComplete { _, ex ->
            if (ex != null) log.warn("Failed to publish user-sync event userId={}: {}", userId, ex.message)
        }
    }

    fun publishUsernameUpdate(userId: Long, newUsername: String) {
        // updatedAt: Consumer 측에서 순서 역전 감지에 사용 — 발행 직전 시각을 기록
        val updatedAt = Instant.now()
        kafkaTemplate.send(
            "user.username.updated", userId.toString(),
            """{"userId":$userId,"newUsername":"$newUsername","updatedAt":"$updatedAt"}"""
        ).whenComplete { _, ex ->
            if (ex != null) log.warn("Failed to publish username-update event userId={}: {}", userId, ex.message)
        }
    }
}
