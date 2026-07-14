package com.example.authorizationserver.presentation

import com.example.authorizationserver.application.service.ApiKeyService
import com.example.authorizationserver.presentation.dto.ApiKeyResponse
import com.example.authorizationserver.presentation.dto.CreateApiKeyRequest
import com.example.authorizationserver.presentation.dto.CreateApiKeyResponse
import com.example.authorizationserver.presentation.dto.toResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/keys")
class ApiKeyController(private val apiKeyService: ApiKeyService) {

    @PostMapping
    fun create(@RequestBody request: CreateApiKeyRequest): ResponseEntity<CreateApiKeyResponse> {
        val (rawKey, apiKey) = apiKeyService.create(
            tenantId = request.tenantId,
            name = request.name,
            rateLimitBurst = request.rateLimitBurst,
            rateLimitRefill = request.rateLimitRefill,
            expiresAt = request.expiresAt
        )
        return ResponseEntity.ok(
            CreateApiKeyResponse(
                id = apiKey.id!!,
                rawKey = rawKey,
                keyPrefix = apiKey.keyPrefix,
                name = apiKey.name,
                rateLimitBurst = apiKey.rateLimitBurst,
                rateLimitRefill = apiKey.rateLimitRefill,
                createdAt = apiKey.createdAt,
                expiresAt = apiKey.expiresAt
            )
        )
    }

    @GetMapping("/tenant/{tenantId}")
    fun list(@PathVariable tenantId: Long): ResponseEntity<List<ApiKeyResponse>> =
        ResponseEntity.ok(apiKeyService.list(tenantId).map { it.toResponse() })

    @DeleteMapping("/{id}")
    fun revoke(@PathVariable id: Long): ResponseEntity<Void> {
        apiKeyService.revoke(id)
        return ResponseEntity.noContent().build()
    }
}
