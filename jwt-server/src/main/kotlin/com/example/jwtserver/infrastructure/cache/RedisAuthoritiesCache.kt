package com.example.jwtserver.infrastructure.cache

import com.example.jwtserver.application.port.out.AuthoritiesCachePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisAuthoritiesCache(
    private val redisTemplate: RedisTemplate<String, Any>
) : AuthoritiesCachePort {

    companion object {
        private const val AUTHORITIES_PREFIX = "jwt:authorities:"
    }

    @Suppress("UNCHECKED_CAST")
    override fun getAuthorities(username: String): Set<String>? =
        redisTemplate.opsForValue().get("$AUTHORITIES_PREFIX$username") as? Set<String>
}
