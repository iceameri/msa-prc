package com.example.opaqueserver.presentation

import com.example.opaqueserver.application.service.ReportProcessingService
import com.example.opaqueserver.domain.report.Report
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
class ReportController(private val reportProcessingService: ReportProcessingService) {

    @GetMapping
    fun getPending(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<Report> = reportProcessingService.getPending(offset, limit)

    @PostMapping("/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun dismiss(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal, @PathVariable id: Long) {
        val actorId = principal.getAttribute<Any>("sub")?.toString() ?: ""
        reportProcessingService.dismiss(actorId, principal.name, id)
    }

    @PostMapping("/{id}/action")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun action(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal, @PathVariable id: Long) {
        val actorId = principal.getAttribute<Any>("sub")?.toString() ?: ""
        reportProcessingService.action(actorId, principal.name, id)
    }
}
