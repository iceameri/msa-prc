package com.example.authorizationserver.application.service

import com.example.authorizationserver.domain.apikey.ApiKey
import com.example.authorizationserver.domain.apikey.ApiKeyRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val redis: StringRedisTemplate
) {

    fun create(tenantId: Long, name: String, rateLimitBurst: Int, rateLimitRefill: Int, expiresAt: Instant? = null): Pair<String, ApiKey> {
        val rawBytes = ByteArray(36).also { SecureRandom().nextBytes(it) }
        val suffix = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes)
        val keyPrefix = suffix.take(8)
        val rawKey = "sk_${keyPrefix}_$suffix"
        val keyHash = sha256Hex(rawKey)

        val saved = apiKeyRepository.save(
            ApiKey(
                tenantId = tenantId,
                keyHash = keyHash,
                keyPrefix = keyPrefix,
                name = name,
                rateLimitBurst = rateLimitBurst,
                rateLimitRefill = rateLimitRefill,
                expiresAt = expiresAt
            )
        )

        // Store rate limit config in Redis for resource servers
        runCatching {
            redis.opsForValue().set("rl:config:${saved.id}", "${rateLimitBurst}:${rateLimitRefill}")
        }

        return rawKey to saved
    }

    fun list(tenantId: Long): List<ApiKey> = apiKeyRepository.findByTenantId(tenantId)

    fun revoke(id: Long) {
        apiKeyRepository.updateStatus(id, "REVOKED")
        runCatching { redis.delete("rl:config:$id") }
    }

    fun validateAndGet(rawKey: String): ApiKey? {
        val hash = sha256Hex(rawKey)
        val apiKey = apiKeyRepository.findByKeyHash(hash) ?: return null
        if (apiKey.status != "ACTIVE") return null
        if (apiKey.expiresAt?.isBefore(Instant.now()) == true) return null
        runCatching { apiKeyRepository.updateLastUsedAt(apiKey.id!!) }
        return apiKey
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
