package com.example.opaqueserver.application.service

import com.example.opaqueserver.domain.audit.AuditLog
import com.example.opaqueserver.infrastructure.persistence.AuditJdbcRepository
import org.springframework.stereotype.Service

@Service
class AuditService(private val auditRepository: AuditJdbcRepository) {

    fun log(actorId: String, actorUsername: String, action: String, targetType: String? = null, targetId: String? = null, detail: String? = null) {
        auditRepository.save(AuditLog(
            actorId = actorId,
            actorUsername = actorUsername,
            action = action,
            targetType = targetType,
            targetId = targetId,
            detail = detail
        ))
    }

    fun getAll(offset: Int, limit: Int) = auditRepository.findAll(offset, limit)
}
