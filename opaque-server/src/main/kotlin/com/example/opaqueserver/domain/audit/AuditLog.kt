package com.example.opaqueserver.domain.audit

import java.time.Instant

data class AuditLog(
    val id: Long? = null,
    val actorId: String,
    val actorUsername: String,
    val action: String,
    val targetType: String? = null,
    val targetId: String? = null,
    val detail: String? = null,
    val createdAt: Instant = Instant.now()
)
