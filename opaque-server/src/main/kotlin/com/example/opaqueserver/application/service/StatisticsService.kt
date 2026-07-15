package com.example.opaqueserver.application.service

import com.example.opaqueserver.domain.report.ReportStatus
import com.example.opaqueserver.infrastructure.persistence.PaymentJdbcRepository
import com.example.opaqueserver.infrastructure.persistence.ReportJdbcRepository
import org.springframework.stereotype.Service

@Service
class StatisticsService(
    private val reportRepository: ReportJdbcRepository,
    private val paymentRepository: PaymentJdbcRepository
) {

    fun getSummary(): Map<String, Any> = mapOf(
        "reports" to mapOf(
            "pending"   to reportRepository.countByStatus(ReportStatus.PENDING),
            "actioned"  to reportRepository.countByStatus(ReportStatus.ACTIONED),
            "dismissed" to reportRepository.countByStatus(ReportStatus.DISMISSED)
        ),
        "payments" to mapOf(
            "totalRevenue" to paymentRepository.sumCompletedAmount()
        )
    )
}
