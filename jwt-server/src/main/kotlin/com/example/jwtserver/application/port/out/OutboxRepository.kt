package com.example.jwtserver.application.port.out

import com.example.jwtserver.domain.event.OutboxEvent

interface OutboxRepository {
    fun save(event: OutboxEvent)
    fun findAndClaim(limit: Int): List<OutboxEvent>
    fun markSent(id: Long)
    fun unclaim(id: Long)
    fun resetStaleClaims()
    fun deleteProcessed()
}
