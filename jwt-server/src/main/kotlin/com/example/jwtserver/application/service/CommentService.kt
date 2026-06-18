package com.example.jwtserver.application.service

import com.example.jwtserver.application.port.out.OutboxRepository
import com.example.jwtserver.domain.comment.Comment
import com.example.jwtserver.domain.comment.CommentRepository
import com.example.jwtserver.domain.event.OutboxEvent
import com.example.jwtserver.domain.post.PostRepository
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
    fun create(userId: Long, username: String, postId: Long, content: String): Comment {
        userSyncService.sync(userId, username)
        val post = postRepository.findById(postId) ?: throw NoSuchElementException("Post not found: $postId")
        val comment = commentRepository.save(Comment(postId = postId, authorId = userId, authorUsername = username, content = content))
        postRepository.incrementCommentCount(postId)

        val postAuthorUsername = post.authorUsername ?: return comment
        if (postAuthorUsername != username) {
            outboxRepository.save(OutboxEvent(
                aggregateId = comment.id.toString(),
                aggregateType = "COMMENT",
                eventType = "POST_COMMENTED",
                payload = """{"postId":$postId,"commentId":${comment.id},"actorUsername":"$username","targetUsername":"$postAuthorUsername"}"""
            ))
        }
        return comment
    }

    @Transactional
    fun update(userId: Long, username: String, commentId: Long, content: String): Comment {
        userSyncService.sync(userId, username)
        val comment = commentRepository.findById(commentId) ?: throw NoSuchElementException("Comment not found: $commentId")
        check(comment.authorId == userId) { "Forbidden" }
        val updated = comment.copy(content = content)
        commentRepository.update(updated)
        return updated
    }

    @Transactional
    fun delete(userId: Long, username: String, commentId: Long) {
        userSyncService.sync(userId, username)
        val comment = commentRepository.findById(commentId) ?: throw NoSuchElementException("Comment not found: $commentId")
        check(comment.authorId == userId) { "Forbidden" }
        commentRepository.delete(commentId)
        postRepository.decrementCommentCount(comment.postId)
    }

    fun getByPost(postId: Long, offset: Int, limit: Int): List<Comment> =
        commentRepository.findByPostId(postId, offset, limit)
}
