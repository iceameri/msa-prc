package com.example.opaqueserver.application.service

import com.example.opaqueserver.domain.report.ReportRepository
import com.example.opaqueserver.domain.report.ReportStatus
import com.example.opaqueserver.domain.payment.PaymentRepository
import org.springframework.stereotype.Service

@Service
class StatisticsService(
    private val reportRepository: ReportRepository,
    private val paymentRepository: PaymentRepository
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
