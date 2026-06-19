package com.example.jwtserver.domain.post

import java.time.Instant

data class Post(
    val id: Long? = null,
    val authorId: Long? = null,
    val clientId: String? = null,
    val authorUsername: String? = null,
    val title: String,
    val content: String,
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val status: PostStatus = PostStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class PostStatus { ACTIVE, DELETED }
