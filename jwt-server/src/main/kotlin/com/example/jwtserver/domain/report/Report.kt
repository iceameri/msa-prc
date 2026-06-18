package com.example.jwtserver.domain.report

import java.time.Instant

data class Report(
    val id: Long? = null,
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: String,
    val createdAt: Instant = Instant.now()
)

enum class ReportTargetType { POST, COMMENT, USER }
