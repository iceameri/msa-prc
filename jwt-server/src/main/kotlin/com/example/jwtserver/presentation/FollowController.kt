package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.FollowService
import com.example.jwtserver.infrastructure.security.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/follows")
class FollowController(private val followService: FollowService) {

    @PostMapping("/{targetUsername}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun follow(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable targetUsername: String) {
        followService.follow(user.id, user.username, targetUsername)
    }

    @DeleteMapping("/{targetUsername}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun unfollow(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable targetUsername: String) {
        followService.unfollow(user.id, user.username, targetUsername)
    }

    @GetMapping("/{username}/stats")
    fun stats(@PathVariable username: String): Map<String, Int> =
        followService.getFollowStats(username)
}
