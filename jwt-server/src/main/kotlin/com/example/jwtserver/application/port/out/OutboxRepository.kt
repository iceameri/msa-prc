package com.example.jwtserver.application.port.out

import com.example.jwtserver.domain.event.OutboxEvent

interface OutboxRepository {
    fun save(event: OutboxEvent)
    fun findUnpublished(limit: Int): List<OutboxEvent>
    fun markPublished(id: Long)
}
