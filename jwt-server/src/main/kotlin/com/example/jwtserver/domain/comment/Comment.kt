package com.example.jwtserver.domain.comment

import java.time.Instant

data class Comment(
    val id: Long? = null,
    val postId: Long,
    val authorId: Long,
    val authorUsername: String? = null,
    val content: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
