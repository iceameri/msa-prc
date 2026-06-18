package com.example.opaqueserver.domain.report

interface ReportRepository {
    fun findById(id: Long): Report?
    fun findByStatus(status: ReportStatus, offset: Int, limit: Int): List<Report>
    fun existsByExternalId(externalId: Long): Boolean
    fun save(report: Report): Report
    fun updateStatus(id: Long, status: ReportStatus, reviewedBy: String)
    fun countByStatus(status: ReportStatus): Int
}
