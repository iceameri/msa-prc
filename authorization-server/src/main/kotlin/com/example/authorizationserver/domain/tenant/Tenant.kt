package com.example.authorizationserver.domain.tenant

import java.time.Instant

data class Tenant(
    val id: Long? = null,
    val name: String,
    val slug: String,
    val status: String = "ACTIVE",
    val createdAt: Instant = Instant.now()
)
