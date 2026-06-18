package com.example.opaqueserver.presentation

import com.example.opaqueserver.application.service.UserManagementService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
class UserManagementController(private val userManagementService: UserManagementService) {

    @PostMapping("/{userId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun suspend(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal, @PathVariable userId: Long) {
        val actorId = principal.getAttribute<Any>("sub")?.toString() ?: ""
        userManagementService.suspend(actorId, principal.name, userId)
    }

    @PostMapping("/{userId}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun ban(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal, @PathVariable userId: Long) {
        val actorId = principal.getAttribute<Any>("sub")?.toString() ?: ""
        userManagementService.ban(actorId, principal.name, userId)
    }

    @PostMapping("/{userId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun restore(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal, @PathVariable userId: Long) {
        val actorId = principal.getAttribute<Any>("sub")?.toString() ?: ""
        userManagementService.restore(actorId, principal.name, userId)
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal, @PathVariable userId: Long) {
        val actorId = principal.getAttribute<Any>("sub")?.toString() ?: ""
        userManagementService.delete(actorId, principal.name, userId)
    }
}
