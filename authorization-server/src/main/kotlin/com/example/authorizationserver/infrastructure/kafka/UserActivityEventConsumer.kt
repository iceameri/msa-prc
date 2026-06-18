package com.example.authorizationserver.infrastructure.kafka

import com.example.authorizationserver.domain.user.UserActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class UserActivityEventConsumer(
    private val userActivityRepository: UserActivityRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(UserActivityEventConsumer::class.java)

    @KafkaListener(topics = ["user-active"], groupId = "authorization-server-group")
    fun consume(message: String) {
        val userId = parseUserId(message) ?: return
        userActivityRepository.upsert(userId)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseUserId(message: String): Long? {
        return try {
            val payload = objectMapper.readValue(message, Map::class.java) as Map<String, Any?>
            payload["userId"]?.toString()?.toLongOrNull() ?: run {
                log.error("Missing or invalid 'userId' in user-active message")
                null
            }
        } catch (ex: Exception) {
            log.error("Invalid user-active message format: {}", ex.message)
            null
        }
    }
}
