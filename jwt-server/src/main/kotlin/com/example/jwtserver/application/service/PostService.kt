package com.example.jwtserver.application.service

import com.example.jwtserver.application.port.out.OutboxRepository
import com.example.jwtserver.application.port.out.SearchPort
import com.example.jwtserver.domain.event.OutboxEvent
import com.example.jwtserver.domain.hashtag.HashtagRepository
import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.domain.post.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val hashtagRepository: HashtagRepository,
    private val userSyncService: UserSyncService,
    private val searchPort: SearchPort,
    private val outboxRepository: OutboxRepository
) {

    @Transactional
    fun create(userId: Long, username: String, title: String, content: String, imageUrl: String?, hashtags: List<String>): Post {
        userSyncService.sync(userId, username)
        val post = postRepository.save(Post(authorId = userId, authorUsername = username, title = title, content = content, imageUrl = imageUrl))
        syncHashtags(post.id!!, hashtags)
        outboxRepository.save(OutboxEvent(
            aggregateId = post.id.toString(),
            aggregateType = "POST",
            eventType = "POST_CREATED",
            payload = """{"postId":${post.id},"authorUsername":"$username"}"""
        ))
        searchPort.indexPost(post, hashtags)
        return post
    }

    @Transactional
    fun update(userId: Long, username: String, postId: Long, title: String, content: String, imageUrl: String?, hashtags: List<String>): Post {
        userSyncService.sync(userId, username)
        val post = postRepository.findById(postId) ?: throw NoSuchElementException("Post not found: $postId")
        check(post.authorId == userId) { "Forbidden" }
        val updated = post.copy(title = title, content = content, imageUrl = imageUrl)
        postRepository.update(updated)
        hashtagRepository.unlinkFromPost(postId)
        syncHashtags(postId, hashtags)
        searchPort.indexPost(updated, hashtags)
        return updated
    }

    @Transactional
    fun delete(userId: Long, username: String, postId: Long, isAdmin: Boolean = false) {
        userSyncService.sync(userId, username)
        val post = postRepository.findById(postId) ?: throw NoSuchElementException("Post not found: $postId")
        check(post.authorId == userId || isAdmin) { "Forbidden" }
        postRepository.delete(postId)
        searchPort.deletePost(postId)
    }

    fun getById(postId: Long): Post =
        postRepository.findById(postId) ?: throw NoSuchElementException("Post not found: $postId")

    fun getByAuthor(username: String, offset: Int, limit: Int): List<Post> {
        val authorId = userSyncService.resolveId(username)
            ?: throw NoSuchElementException("User not found: $username")
        return postRepository.findByAuthorId(authorId, offset, limit)
    }

    fun getByHashtag(hashtagName: String, offset: Int, limit: Int): List<Post> =
        postRepository.findByHashtag(hashtagName, offset, limit)

    private fun syncHashtags(postId: Long, hashtags: List<String>) {
        hashtags.forEach { name ->
            val hashtag = hashtagRepository.findOrCreate(name)
            hashtagRepository.linkToPost(postId, hashtag.id!!)
        }
    }
}
