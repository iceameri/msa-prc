package com.example.opaqueserver.domain.outbox

import java.time.Instant

data class OutboxEvent(
    val id: Long? = null,
    val topic: String,
    val aggregateKey: String,
    val payload: String,
    val claimedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
)
