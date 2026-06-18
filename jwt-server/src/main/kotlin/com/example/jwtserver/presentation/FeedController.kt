package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.FeedService
import com.example.jwtserver.infrastructure.security.AuthenticatedUser
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/feed")
class FeedController(private val feedService: FeedService) {

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getFeed(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<PostResponse> = feedService.getFeed(user.id, user.username, offset, limit).map { PostResponse.from(it) }
}
