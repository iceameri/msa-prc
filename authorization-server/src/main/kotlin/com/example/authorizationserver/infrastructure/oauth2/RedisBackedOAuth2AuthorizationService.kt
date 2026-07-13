package com.example.authorizationserver.infrastructure.oauth2

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import java.time.Duration
import java.time.Instant

private const val KEY_PREFIX = "oauth2:token:"
private val FALLBACK_TTL = Duration.ofMinutes(30)

class RedisBackedOAuth2AuthorizationService(
    private val delegate: OAuth2AuthorizationService,
    private val redis: StringRedisTemplate,
    cbRegistry: CircuitBreakerRegistry
) : OAuth2AuthorizationService {

    private val cb = cbRegistry.circuitBreaker("authorization-db")

    override fun save(authorization: OAuth2Authorization) {
        cb.executeRunnable { delegate.save(authorization) }
        runCatching { cacheAll(authorization) }
    }

    override fun remove(authorization: OAuth2Authorization) {
        cb.executeRunnable { delegate.remove(authorization) }
        runCatching { evictAll(authorization) }
    }

    override fun findById(id: String): OAuth2Authorization? =
        cb.executeSupplier { delegate.findById(id) }

    override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
        val cachedId = runCatching { redis.opsForValue().get(KEY_PREFIX + token) }.getOrNull()

        if (cachedId != null) {
            val auth = cb.executeSupplier { delegate.findById(cachedId) }
            if (auth == null) {
                // 스테일 캐시 자가 치유
                runCatching { redis.delete(KEY_PREFIX + token) }
            }
            return auth
        }

        // 캐시 미스 → DB 풀쿼리
        val auth = cb.executeSupplier { delegate.findByToken(token, tokenType) }
        if (auth != null) {
            runCatching { cacheAll(auth) }
        }
        return auth  // DB에도 없으면 null 반환 (Spring AS가 invalid token으로 처리)
    }

    private fun cacheAll(auth: OAuth2Authorization) {
        val ttl = resolveTtl(auth)
        tokenValues(auth).forEach { value ->
            redis.opsForValue().set(KEY_PREFIX + value, auth.id, ttl)
        }
    }

    private fun evictAll(auth: OAuth2Authorization) {
        val keys = tokenValues(auth).map { KEY_PREFIX + it }
        if (keys.isNotEmpty()) redis.delete(keys)
    }

    private fun tokenValues(auth: OAuth2Authorization): List<String> = listOfNotNull(
        auth.getAccessToken()?.token?.tokenValue,
        auth.getRefreshToken()?.token?.tokenValue,
        auth.getToken(OAuth2AuthorizationCode::class.java)?.token?.tokenValue,
        auth.getToken(OidcIdToken::class.java)?.token?.tokenValue,
    )

    private fun resolveTtl(auth: OAuth2Authorization): Duration {
        val expiresAt = auth.getRefreshToken()?.token?.expiresAt
            ?: auth.getAccessToken()?.token?.expiresAt
        val ttl = expiresAt?.let { Duration.between(Instant.now(), it) }
        return if (ttl == null || ttl.isNegative || ttl.isZero) FALLBACK_TTL else ttl
    }
}
