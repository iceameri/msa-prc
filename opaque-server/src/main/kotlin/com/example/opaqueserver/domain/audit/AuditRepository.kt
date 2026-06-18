package com.example.opaqueserver.domain.audit

interface AuditRepository {
    fun save(log: AuditLog)
    fun findAll(offset: Int, limit: Int): List<AuditLog>
}
