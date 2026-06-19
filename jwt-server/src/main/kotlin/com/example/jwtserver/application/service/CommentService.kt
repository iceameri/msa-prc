package com.example.jwtserver.application.service

import com.example.jwtserver.application.port.out.OutboxRepository
import com.example.jwtserver.domain.comment.Comment
import com.example.jwtserver.domain.comment.CommentRepository
import com.example.jwtserver.domain.event.OutboxEvent
import com.example.jwtserver.domain.post.PostRepository
import com.example.jwtserver.infrastructure.security.AuthenticatedClient
import com.example.jwtserver.infrastructure.security.AuthenticatedUser
import com.example.jwtserver.infrastructure.security.CallerPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userSyncService: UserSyncService,
    private val outboxRepository: OutboxRepository
) {

    @Transactional
    fun create(caller: CallerPrincipal, postId: Long, content: String): Comment {
        val (authorId, clientId, displayName) = resolveAuthor(caller)
        val post = postRepository.findById(postId) ?: throw NoSuchElementException("Post not found: $postId")
        val comment = commentRepository.save(Comment(postId = postId, authorId = authorId, clientId = clientId, content = content))
        postRepository.incrementCommentCount(postId)

        val postAuthorUsername = post.authorUsername ?: return comment
        if (postAuthorUsername != displayName) {
            outboxRepository.save(OutboxEvent(
                aggregateId = comment.id.toString(),
                aggregateType = "COMMENT",
                eventType = "POST_COMMENTED",
                payload = """{"postId":$postId,"commentId":${comment.id},"actorUsername":"$displayName","targetUsername":"$postAuthorUsername"}"""
            ))
        }
        return comment
    }

    @Transactional
    fun update(caller: CallerPrincipal, commentId: Long, content: String): Comment {
        val comment = commentRepository.findById(commentId) ?: throw NoSuchElementException("Comment not found: $commentId")
        check(canModify(caller, comment)) { "Forbidden" }
        if (caller is AuthenticatedUser) userSyncService.sync(caller.id, caller.username)
        val updated = comment.copy(content = content)
        commentRepository.update(updated)
        return updated
    }

    @Transactional
    fun delete(caller: CallerPrincipal, commentId: Long) {
        val comment = commentRepository.findById(commentId) ?: throw NoSuchElementException("Comment not found: $commentId")
        check(canModify(caller, comment)) { "Forbidden" }
        if (caller is AuthenticatedUser) userSyncService.sync(caller.id, caller.username)
        commentRepository.delete(commentId)
        postRepository.decrementCommentCount(comment.postId)
    }

    fun getByPost(postId: Long, offset: Int, limit: Int): List<Comment> =
        commentRepository.findByPostId(postId, offset, limit)

    private fun resolveAuthor(caller: CallerPrincipal): Triple<Long?, String?, String> = when (caller) {
        is AuthenticatedUser -> {
            userSyncService.sync(caller.id, caller.username)
            Triple(caller.id, null, caller.username)
        }
        is AuthenticatedClient -> Triple(null, caller.clientId, caller.clientId)
        else -> Triple(null, null, "unknown_client")
    }

    private fun canModify(caller: CallerPrincipal, comment: Comment): Boolean = when (caller) {
        is AuthenticatedUser -> comment.authorId == caller.id
        is AuthenticatedClient -> comment.clientId == caller.clientId
        else -> false
    }
}
