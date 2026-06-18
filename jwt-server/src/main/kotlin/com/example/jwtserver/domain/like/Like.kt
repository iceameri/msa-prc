package com.example.jwtserver.domain.like

import java.time.Instant

data class Like(
    val postId: Long,
    val userId: Long,
    val createdAt: Instant = Instant.now()
)
