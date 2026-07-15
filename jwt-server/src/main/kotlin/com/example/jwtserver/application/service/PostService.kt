package com.example.jwtserver.application.service

import com.example.jwtserver.application.port.out.OutboxRepository
import com.example.jwtserver.application.port.out.SearchPort
import com.example.jwtserver.domain.event.OutboxEvent
import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.infrastructure.persistence.HashtagJdbcRepository
import com.example.jwtserver.infrastructure.persistence.PostJdbcRepository
import com.example.jwtserver.infrastructure.security.AuthenticatedClient
import com.example.jwtserver.infrastructure.security.AuthenticatedUser
import com.example.jwtserver.infrastructure.security.CallerPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostJdbcRepository,
    private val hashtagRepository: HashtagJdbcRepository,
    private val userSyncService: UserSyncService,
    private val searchPort: SearchPort,
    private val outboxRepository: OutboxRepository
) {

    @Transactional
    fun create(caller: CallerPrincipal, title: String, content: String, imageUrl: String?, hashtags: List<String>): Post {
        val (authorId, clientId, displayName) = resolveAuthor(caller)
        val post = postRepository.save(Post(authorId = authorId, clientId = clientId, title = title, content = content, imageUrl = imageUrl))
        syncHashtags(post.id!!, hashtags)
        outboxRepository.save(OutboxEvent(
            aggregateId = post.id.toString(),
            aggregateType = "POST",
            eventType = "POST_CREATED",
            payload = """{"postId":${post.id},"authorUsername":"$displayName"}"""
        ))
        searchPort.indexPost(post, hashtags)
        return post
    }

    @Transactional
    fun update(caller: CallerPrincipal, postId: Long, title: String, content: String, imageUrl: String?, hashtags: List<String>): Post {
        val post = postRepository.findById(postId) ?: throw NoSuchElementException("Post not found: $postId")
        check(canModify(caller, post)) { "Forbidden" }
        if (caller is AuthenticatedUser) userSyncService.sync(caller.id, caller.username)
        val updated = post.copy(title = title, content = content, imageUrl = imageUrl)
        postRepository.update(updated)
        hashtagRepository.unlinkFromPost(postId)
        syncHashtags(postId, hashtags)
        searchPort.indexPost(updated, hashtags)
        return updated
    }

    @Transactional
    fun delete(caller: CallerPrincipal, postId: Long) {
        val post = postRepository.findById(postId) ?: throw NoSuchElementException("Post not found: $postId")
        check(canModify(caller, post)) { "Forbidden" }
        if (caller is AuthenticatedUser) userSyncService.sync(caller.id, caller.username)
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

    private fun resolveAuthor(caller: CallerPrincipal): Triple<Long?, String?, String> = when (caller) {
        is AuthenticatedUser -> {
            userSyncService.sync(caller.id, caller.username)
            Triple(caller.id, null, caller.username)
        }
        is AuthenticatedClient -> Triple(null, caller.clientId, caller.clientId)
        else -> Triple(null, null, "unknown_client")
    }

    private fun canModify(caller: CallerPrincipal, post: Post): Boolean = when (caller) {
        is AuthenticatedUser -> post.authorId == caller.id || caller.roles.contains("ROLE_ADMIN")
        is AuthenticatedClient -> post.clientId == caller.clientId
        else -> false
    }

    private fun syncHashtags(postId: Long, hashtags: List<String>) {
        hashtags.forEach { name ->
            val hashtag = hashtagRepository.findOrCreate(name)
            hashtagRepository.linkToPost(postId, hashtag.id!!)
        }
    }
}
