package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.LikeService
import com.example.jwtserver.infrastructure.security.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/posts/{postId}/likes")
class LikeController(private val likeService: LikeService) {

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    fun like(@AuthenticationPrincipal user: AuthenticatedUser?, @PathVariable postId: Long) {
        user ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "User context required")
        likeService.like(user.id, user.username, postId)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    fun unlike(@AuthenticationPrincipal user: AuthenticatedUser?, @PathVariable postId: Long) {
        user ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "User context required")
        likeService.unlike(user.id, user.username, postId)
    }
}
