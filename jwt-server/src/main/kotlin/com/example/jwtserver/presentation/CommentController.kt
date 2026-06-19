package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.CommentService
import com.example.jwtserver.domain.comment.Comment
import com.example.jwtserver.infrastructure.security.CallerPrincipal
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class CreateCommentRequest(val content: String)
data class UpdateCommentRequest(val content: String)
data class CommentResponse(val id: Long, val postId: Long, val authorId: Long?, val authorUsername: String?, val content: String, val createdAt: String) {
    companion object {
        fun from(c: Comment) = CommentResponse(c.id!!, c.postId, c.authorId, c.authorUsername, c.content, c.createdAt.toString())
    }
}

@RestController
@RequestMapping("/api/comments")
class CommentController(private val commentService: CommentService) {

    @PostMapping("/post/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    fun create(@AuthenticationPrincipal caller: CallerPrincipal, @PathVariable postId: Long, @RequestBody req: CreateCommentRequest): CommentResponse =
        CommentResponse.from(commentService.create(caller, postId, req.content))

    @GetMapping("/post/{postId}")
    fun getByPost(
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<CommentResponse> = commentService.getByPost(postId, offset, limit).map { CommentResponse.from(it) }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    fun update(@AuthenticationPrincipal caller: CallerPrincipal, @PathVariable id: Long, @RequestBody req: UpdateCommentRequest): CommentResponse =
        CommentResponse.from(commentService.update(caller, id, req.content))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    fun delete(@AuthenticationPrincipal caller: CallerPrincipal, @PathVariable id: Long) {
        commentService.delete(caller, id)
    }
}
