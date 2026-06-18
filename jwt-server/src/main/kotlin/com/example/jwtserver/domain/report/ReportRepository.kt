package com.example.jwtserver.domain.report

interface ReportRepository {
    fun save(report: Report): Report
}
