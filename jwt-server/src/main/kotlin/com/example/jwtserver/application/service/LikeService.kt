package com.example.jwtserver.application.service

import com.example.jwtserver.application.port.out.OutboxRepository
import com.example.jwtserver.domain.event.OutboxEvent
import com.example.jwtserver.domain.like.Like
import com.example.jwtserver.domain.like.LikeRepository
import com.example.jwtserver.domain.post.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LikeService(
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
    private val userSyncService: UserSyncService,
    private val outboxRepository: OutboxRepository
) {

    @Transactional
    fun like(userId: Long, username: String, postId: Long) {
        userSyncService.sync(userId, username)
        val post = postRepository.findById(postId) ?: throw NoSuchElementException("Post not found: $postId")
        check(!likeRepository.exists(postId, userId)) { "Already liked" }

        likeRepository.save(Like(postId = postId, userId = userId))
        postRepository.incrementLikeCount(postId)

        val postAuthorUsername = post.authorUsername ?: return
        if (postAuthorUsername != username) {
            outboxRepository.save(OutboxEvent(
                aggregateId = postId.toString(),
                aggregateType = "POST",
                eventType = "POST_LIKED",
                payload = """{"postId":$postId,"actorUsername":"$username","targetUsername":"$postAuthorUsername"}"""
            ))
        }
    }

    @Transactional
    fun unlike(userId: Long, username: String, postId: Long) {
        userSyncService.sync(userId, username)
        check(likeRepository.exists(postId, userId)) { "Not liked" }
        likeRepository.delete(postId, userId)
        postRepository.decrementLikeCount(postId)
    }
}
