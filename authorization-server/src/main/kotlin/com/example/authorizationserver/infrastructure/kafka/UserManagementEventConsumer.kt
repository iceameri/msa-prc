package com.example.authorizationserver.infrastructure.kafka

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.infrastructure.oauth2.TokenRevocationService
import com.example.authorizationserver.infrastructure.persistence.UserJdbcRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class UserManagementEventConsumer(
    private val userRepository: UserJdbcRepository,
    private val userCachePort: UserCachePort,
    private val objectMapper: ObjectMapper,
    private val tokenRevocationService: TokenRevocationService
) {
    private val log = LoggerFactory.getLogger(UserManagementEventConsumer::class.java)

    @KafkaListener(topics = ["user-management"], groupId = "authorization-server-group")
    fun consume(message: String) {
        val (action, userId) = parseMessage(message) ?: return

        when (action) {
            "SUSPEND" -> {
                userRepository.setStatusAndEnabled(userId, false, "SUSPENDED")
                evictAndRevoke(userId)
                log.info("User suspended: userId={}", userId)
            }
            "BAN" -> {
                userRepository.setStatusAndEnabled(userId, false, "BANNED")
                evictAndRevoke(userId)
                log.info("User banned: userId={}", userId)
            }
            "DELETE" -> {
                userRepository.setStatusAndEnabled(userId, false, "DELETED")
                evictAndRevoke(userId)
                log.info("User deleted: userId={}", userId)
            }
            "RESTORE" -> {
                userRepository.setStatusAndEnabled(userId, true, "ACTIVE")
                val user = userRepository.findById(userId)
                if (user != null) {
                    if (user.tenantId != null) userCachePort.deleteUser(user.username, user.tenantId)
                    else userCachePort.deleteUser(user.username)
                }
                log.info("User restored: userId={}", userId)
            }
            else -> log.warn("Unknown user-management action: {}", action)
        }
    }

    private fun evictAndRevoke(userId: Long) {
        val user = userRepository.findById(userId) ?: return
        userCachePort.deleteAuthorities(userId)
        if (user.tenantId != null) userCachePort.deleteUser(user.username, user.tenantId)
        else userCachePort.deleteUser(user.username)
        tokenRevocationService.revokeAllForPrincipal(user.username)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMessage(message: String): Pair<String, Long>? {
        return try {
            val payload = objectMapper.readValue(message, Map::class.java) as Map<String, Any?>
            val action = payload["action"]?.toString() ?: run {
                log.error("Missing 'action' in user-management message")
                return null
            }
            val userId = payload["userId"]?.toString()?.toLongOrNull() ?: run {
                log.error("Missing or invalid 'userId' in user-management message")
                return null
            }
            action to userId
        } catch (ex: Exception) {
            log.error("Invalid user-management message format, skipping: {}", ex.message)
            null
        }
    }
}
