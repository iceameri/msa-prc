package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.LikeService
import com.example.jwtserver.infrastructure.security.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts/{postId}/likes")
class LikeController(private val likeService: LikeService) {

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun like(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable postId: Long) {
        likeService.like(user.id, user.username, postId)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun unlike(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable postId: Long) {
        likeService.unlike(user.id, user.username, postId)
    }
}
