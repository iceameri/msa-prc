package com.example.authorizationserver.presentation

import com.example.authorizationserver.infrastructure.batch.InactiveUserCleanupService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/batch")
@PreAuthorize("hasRole('ADMIN')")
class BatchController(
    private val inactiveUserCleanupService: InactiveUserCleanupService
) {

    @PostMapping("/inactive-user-cleanup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun runInactiveUserCleanup(): Map<String, Any> {
        val count = inactiveUserCleanupService.run()
        return mapOf("queued" to count, "status" to "COMPLETED")
    }
}
