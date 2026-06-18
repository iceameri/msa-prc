package com.example.opaqueserver.application.service

import com.example.opaqueserver.application.port.out.EventPublishPort
import com.example.opaqueserver.domain.report.Report
import com.example.opaqueserver.domain.report.ReportRepository
import com.example.opaqueserver.domain.report.ReportStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportProcessingService(
    private val reportRepository: ReportRepository,
    private val auditService: AuditService,
    private val eventPublishPort: EventPublishPort
) {

    fun receiveFromKafka(externalId: Long, reporterUsername: String, targetType: String, targetId: Long, reason: String) {
        if (reportRepository.existsByExternalId(externalId)) return
        reportRepository.save(Report(
            externalId = externalId,
            reporterUsername = reporterUsername,
            targetType = targetType,
            targetId = targetId,
            reason = reason
        ))
    }

    @Transactional
    fun dismiss(actorId: String, actorUsername: String, reportId: Long) {
        val report = reportRepository.findById(reportId) ?: throw NoSuchElementException("Report not found: $reportId")
        check(report.status == ReportStatus.PENDING) { "Report is not pending" }
        reportRepository.updateStatus(reportId, ReportStatus.DISMISSED, actorUsername)
        auditService.log(actorId, actorUsername, "DISMISS_REPORT", "REPORT", reportId.toString())
    }

    @Transactional
    fun action(actorId: String, actorUsername: String, reportId: Long) {
        val report = reportRepository.findById(reportId) ?: throw NoSuchElementException("Report not found: $reportId")
        check(report.status == ReportStatus.PENDING) { "Report is not pending" }
        reportRepository.updateStatus(reportId, ReportStatus.ACTIONED, actorUsername)
        auditService.log(actorId, actorUsername, "ACTION_REPORT", "REPORT", reportId.toString())
        eventPublishPort.publish("report-actions", reportId.toString(),
            """{"reportId":$reportId,"targetType":"${report.targetType}","targetId":${report.targetId},"action":"ACTIONED","actor":"$actorUsername"}""")
    }

    fun getPending(offset: Int, limit: Int) = reportRepository.findByStatus(ReportStatus.PENDING, offset, limit)
}
