package com.example.authorizationserver.infrastructure.cache

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.domain.user.User
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisUserCache(
    private val redisTemplate: RedisTemplate<String, Any>
) : UserCachePort {

    companion object {
        private const val USER_PREFIX = "auth:user:"
        private const val ATTEMPTS_PREFIX = "auth:attempts:"
        private const val AUTHORITIES_PREFIX = "jwt:authorities:"
        private const val MFA_PENDING_PREFIX = "auth:mfa:pending:"
        private val USER_TTL = Duration.ofMinutes(30)
        private val ATTEMPTS_TTL = Duration.ofMinutes(30)
        private val AUTHORITIES_TTL = Duration.ofHours(24)
        private val MFA_PENDING_TTL = Duration.ofMinutes(10)
    }

    private fun userKey(username: String, tenantId: Long? = null): String =
        if (tenantId != null) "$USER_PREFIX$tenantId:$username" else "$USER_PREFIX$username"

    override fun getUser(username: String): User? =
        redisTemplate.opsForValue().get(userKey(username)) as? User

    override fun getUser(username: String, tenantId: Long): User? =
        redisTemplate.opsForValue().get(userKey(username, tenantId)) as? User

    override fun saveUser(user: User) {
        redisTemplate.opsForValue().set(userKey(user.username, user.tenantId), user, USER_TTL)
    }

    override fun deleteUser(username: String) {
        redisTemplate.delete(userKey(username))
    }

    override fun deleteUser(username: String, tenantId: Long) {
        redisTemplate.delete(userKey(username, tenantId))
    }

    override fun deleteAuthorities(username: String) {
        redisTemplate.delete("$AUTHORITIES_PREFIX$username")
    }

    override fun getLoginAttempts(username: String): Int =
        (redisTemplate.opsForValue().get("$ATTEMPTS_PREFIX$username") as? Number)?.toInt() ?: 0

    override fun incrementLoginAttempts(username: String): Int {
        val key = "$ATTEMPTS_PREFIX$username"
        val attempts = redisTemplate.opsForValue().increment(key)?.toInt() ?: 1
        redisTemplate.expire(key, ATTEMPTS_TTL)
        return attempts
    }

    override fun resetLoginAttempts(username: String) {
        redisTemplate.delete("$ATTEMPTS_PREFIX$username")
    }

    override fun saveAuthorities(username: String, authorities: Set<String?>) {
        redisTemplate.opsForValue().set("$AUTHORITIES_PREFIX$username", authorities, AUTHORITIES_TTL)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getAuthorities(username: String): Set<String>? =
        redisTemplate.opsForValue().get("$AUTHORITIES_PREFIX$username") as? Set<String>

    override fun savePendingMfaSecret(username: String, secret: String) {
        redisTemplate.opsForValue().set("$MFA_PENDING_PREFIX$username", secret, MFA_PENDING_TTL)
    }

    override fun getPendingMfaSecret(username: String): String? =
        redisTemplate.opsForValue().get("$MFA_PENDING_PREFIX$username") as? String

    override fun deletePendingMfaSecret(username: String) {
        redisTemplate.delete("$MFA_PENDING_PREFIX$username")
    }
}
