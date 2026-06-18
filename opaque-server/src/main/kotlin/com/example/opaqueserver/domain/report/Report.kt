package com.example.opaqueserver.domain.report

import java.time.Instant

data class Report(
    val id: Long? = null,
    val externalId: Long,
    val reporterUsername: String,
    val targetType: String,
    val targetId: Long,
    val reason: String,
    val status: ReportStatus = ReportStatus.PENDING,
    val reviewedBy: String? = null,
    val reviewedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
)

enum class ReportStatus { PENDING, REVIEWED, DISMISSED, ACTIONED }
