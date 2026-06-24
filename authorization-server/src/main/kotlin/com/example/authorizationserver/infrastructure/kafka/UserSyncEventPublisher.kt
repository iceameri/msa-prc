package com.example.authorizationserver.infrastructure.kafka

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Suppress("MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_WARNING")
@Component
class UserSyncEventPublisher(private val kafkaTemplate: KafkaTemplate<String, String>) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(userId: Long, username: String, version: Long) {
        kafkaTemplate.send(
            "user-sync", userId.toString(),
            """{"userId":$userId,"username":"$username","version":$version}"""
        ).whenComplete { _, ex ->
            if (ex != null) log.warn("Failed to publish user-sync event userId={}: {}", userId, ex.message)
        }
    }

    fun publishUsernameUpdate(userId: Long, newUsername: String, version: Long) {
        kafkaTemplate.send(
            "user.username.updated", userId.toString(),
            """{"userId":$userId,"newUsername":"$newUsername","version":$version}"""
        ).whenComplete { _, ex ->
            if (ex != null) log.warn("Failed to publish username-update event userId={}: {}", userId, ex.message)
        }
    }
}
