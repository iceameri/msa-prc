package com.example.authorizationserver.domain.apikey

import java.time.Instant

data class ApiKey(
    val id: Long? = null,
    val tenantId: Long,
    val keyHash: String,
    val keyPrefix: String,
    val name: String,
    val status: String = "ACTIVE",
    val rateLimitBurst: Int = 100,
    val rateLimitRefill: Int = 50,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant? = null,
    val lastUsedAt: Instant? = null
)
