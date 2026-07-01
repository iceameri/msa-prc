package com.example.jwtserver.domain.user

import java.time.Instant

data class User(
    val id: Long,
    val username: String,
    val enabled: Boolean,
    val status: String,
    val version: Long,
    val createdAt: Instant
)
