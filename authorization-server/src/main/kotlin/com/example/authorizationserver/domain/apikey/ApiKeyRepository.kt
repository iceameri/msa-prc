package com.example.authorizationserver.domain.apikey

interface ApiKeyRepository {
    fun findByKeyHash(keyHash: String): ApiKey?
    fun findById(id: Long): ApiKey?
    fun findByTenantId(tenantId: Long): List<ApiKey>
    fun save(apiKey: ApiKey): ApiKey
    fun updateStatus(id: Long, status: String)
    fun updateLastUsedAt(id: Long)
}
