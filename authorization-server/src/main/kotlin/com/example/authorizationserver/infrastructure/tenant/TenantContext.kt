package com.example.authorizationserver.infrastructure.tenant

object TenantContext {
    private val tenantId = ThreadLocal<Long?>()

    fun set(id: Long?) = tenantId.set(id)
    fun get(): Long? = tenantId.get()
    fun clear() = tenantId.remove()
}
