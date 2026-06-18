package com.example.jwtserver.domain.user

import java.time.Instant

data class User(
    val id: Long,
    val username: String,
    val createdAt: Instant = Instant.now()
)
