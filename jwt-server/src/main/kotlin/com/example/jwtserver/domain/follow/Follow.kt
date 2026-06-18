package com.example.jwtserver.domain.follow

import java.time.Instant

data class Follow(
    val followerId: Long,
    val followingId: Long,
    val createdAt: Instant = Instant.now()
)
