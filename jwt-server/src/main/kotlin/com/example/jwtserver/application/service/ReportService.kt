package com.example.jwtserver.application.service

import com.example.jwtserver.infrastructure.persistence.OutboxJdbcRepository
import com.example.jwtserver.domain.event.OutboxEvent
import com.example.jwtserver.domain.report.Report
import com.example.jwtserver.domain.report.ReportTargetType
import com.example.jwtserver.infrastructure.persistence.ReportJdbcRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportService(
    private val reportRepository: ReportJdbcRepository,
    private val userSyncService: UserSyncService,
    private val outboxRepository: OutboxJdbcRepository
) {

    @Transactional
    fun report(userId: Long, username: String, targetType: ReportTargetType, targetId: Long, reason: String): Report {
        userSyncService.sync(userId, username)
        val report = reportRepository.save(
            Report(reporterId = userId, targetType = targetType, targetId = targetId, reason = reason)
        )
        val payload = """{"reportId":${report.id},"targetType":"$targetType","targetId":$targetId,"reporterUsername":"$username"}"""
        outboxRepository.save(OutboxEvent(
            aggregateId = report.id.toString(),
            aggregateType = "REPORT",
            eventType = "REPORT_CREATED",
            payload = payload
        ))
        return report
    }
}
