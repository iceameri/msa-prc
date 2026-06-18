package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.ReportService
import com.example.jwtserver.domain.report.Report
import com.example.jwtserver.domain.report.ReportTargetType
import com.example.jwtserver.infrastructure.security.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class CreateReportRequest(val targetType: ReportTargetType, val targetId: Long, val reason: String)
data class ReportResponse(val id: Long, val targetType: String, val targetId: Long, val createdAt: String) {
    companion object {
        fun from(r: Report) = ReportResponse(r.id!!, r.targetType.name, r.targetId, r.createdAt.toString())
    }
}

@RestController
@RequestMapping("/api/reports")
class ReportController(private val reportService: ReportService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun report(@AuthenticationPrincipal user: AuthenticatedUser, @RequestBody req: CreateReportRequest): ReportResponse =
        ReportResponse.from(reportService.report(user.id, user.username, req.targetType, req.targetId, req.reason))
}
