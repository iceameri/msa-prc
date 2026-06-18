package com.example.authorizationserver.domain.user

import java.time.Instant

data class User(
    val id: Long? = null,
    val username: String,
    val password: String,
    val email: String,
    val fullName: String? = null,
    val authorities: Set<String> = emptySet(),
    val enabled: Boolean = true,
    val status: String = "ACTIVE",
    val loginAttempts: Int = 0,
    val lockedUntil: Instant? = null,
    val lastActiveAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val mfaEnabled: Boolean = false,
    val mfaSecret: String? = null
)
