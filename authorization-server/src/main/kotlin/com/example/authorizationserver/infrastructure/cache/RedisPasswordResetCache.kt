package com.example.authorizationserver.infrastructure.cache

import com.example.authorizationserver.application.port.out.PasswordResetCachePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisPasswordResetCache(
    private val redisTemplate: RedisTemplate<String, Any>
) : PasswordResetCachePort {

    companion object {
        private const val RESET_PREFIX = "auth:reset:"
        private val RESET_TTL = Duration.ofMinutes(30)
    }

    override fun saveToken(token: String, username: String) {
        redisTemplate.opsForValue().set("$RESET_PREFIX$token", username, RESET_TTL)
    }

    override fun getUsernameByToken(token: String): String? =
        redisTemplate.opsForValue().get("$RESET_PREFIX$token") as? String

    override fun deleteToken(token: String) {
        redisTemplate.delete("$RESET_PREFIX$token")
    }
}
