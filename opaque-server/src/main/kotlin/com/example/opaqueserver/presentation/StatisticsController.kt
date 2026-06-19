package com.example.opaqueserver.presentation

import com.example.opaqueserver.application.service.AuditService
import com.example.opaqueserver.application.service.StatisticsService
import com.example.opaqueserver.domain.audit.AuditLog
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
class StatisticsController(
    private val statisticsService: StatisticsService,
    private val auditService: AuditService
) {

    @GetMapping("/statistics")
    fun getSummary(): Map<String, Any> = statisticsService.getSummary()

    @GetMapping("/audit-logs")
    fun getAuditLogs(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<AuditLog> = auditService.getAll(offset, limit)
}
