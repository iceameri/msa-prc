package com.example.jwtserver.infrastructure.kafka

import com.example.jwtserver.application.service.UserSyncService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class UsernameUpdateEventConsumer(
    private val userSyncService: UserSyncService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["user.username.updated"], groupId = "jwt-server-group")
    fun consume(message: String) {
        try {
            @Suppress("UNCHECKED_CAST")
            val payload = objectMapper.readValue(message, Map::class.java) as Map<String, Any?>
            val userId = payload["userId"]?.toString()?.toLongOrNull() ?: run {
                log.error("Missing or invalid userId in user.username.updated message")
                return
            }
            val newUsername = payload["newUsername"]?.toString() ?: run {
                log.error("Missing newUsername in user.username.updated message")
                return
            }
            userSyncService.sync(userId, newUsername)
        } catch (ex: Exception) {
            log.error("Invalid user.username.updated message format: {}", ex.message)
        }
    }
}
