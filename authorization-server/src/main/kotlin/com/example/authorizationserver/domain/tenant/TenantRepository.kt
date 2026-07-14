package com.example.authorizationserver.domain.tenant

interface TenantRepository {
    fun findBySlug(slug: String): Tenant?
    fun findById(id: Long): Tenant?
    fun save(tenant: Tenant): Tenant
}
