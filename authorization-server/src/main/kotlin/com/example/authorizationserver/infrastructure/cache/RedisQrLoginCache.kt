package com.example.authorizationserver.infrastructure.cache

import com.example.authorizationserver.application.port.out.QrLoginCachePort
import com.example.authorizationserver.domain.qr.QrLoginSession
import com.example.authorizationserver.domain.qr.QrLoginStatus
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisQrLoginCache(
    private val redisTemplate: RedisTemplate<String, Any>
) : QrLoginCachePort {

    companion object {
        private const val QR_PREFIX = "auth:qr:"
        private val QR_TTL = Duration.ofMinutes(5)
    }

    override fun save(session: QrLoginSession) {
        redisTemplate.opsForValue().set("$QR_PREFIX${session.token}", session, QR_TTL)
    }

    override fun get(token: String): QrLoginSession? =
        redisTemplate.opsForValue().get("$QR_PREFIX$token") as? QrLoginSession

    override fun confirm(token: String, username: String) {
        val session = get(token) ?: return
        val ttl = redisTemplate.getExpire("$QR_PREFIX$token")
        val remaining = if (ttl > 0) Duration.ofSeconds(ttl) else QR_TTL
        redisTemplate.opsForValue().set(
            "$QR_PREFIX$token",
            session.copy(status = QrLoginStatus.CONFIRMED, username = username),
            remaining
        )
    }

    override fun delete(token: String) {
        redisTemplate.delete("$QR_PREFIX$token")
    }
}
