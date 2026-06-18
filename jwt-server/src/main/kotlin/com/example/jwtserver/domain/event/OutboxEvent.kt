package com.example.jwtserver.domain.event

import java.time.Instant

data class OutboxEvent(
    val id: Long? = null,
    val aggregateId: String,
    val aggregateType: String,
    val eventType: String,
    val payload: String,
    val published: Boolean = false,
    val createdAt: Instant = Instant.now()
)
