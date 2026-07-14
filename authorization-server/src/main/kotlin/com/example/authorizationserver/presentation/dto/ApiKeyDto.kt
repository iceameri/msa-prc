package com.example.authorizationserver.presentation.dto

import com.example.authorizationserver.domain.apikey.ApiKey
import java.time.Instant

data class CreateApiKeyRequest(
    val tenantId: Long,
    val name: String,
    val rateLimitBurst: Int = 100,
    val rateLimitRefill: Int = 50,
    val expiresAt: Instant? = null
)

data class CreateApiKeyResponse(
    val id: Long,
    val rawKey: String,
    val keyPrefix: String,
    val name: String,
    val rateLimitBurst: Int,
    val rateLimitRefill: Int,
    val createdAt: Instant,
    val expiresAt: Instant?
)

data class ApiKeyResponse(
    val id: Long,
    val keyPrefix: String,
    val name: String,
    val status: String,
    val rateLimitBurst: Int,
    val rateLimitRefill: Int,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val lastUsedAt: Instant?
)

fun ApiKey.toResponse() = ApiKeyResponse(
    id = id!!,
    keyPrefix = keyPrefix,
    name = name,
    status = status,
    rateLimitBurst = rateLimitBurst,
    rateLimitRefill = rateLimitRefill,
    createdAt = createdAt,
    expiresAt = expiresAt,
    lastUsedAt = lastUsedAt
)
