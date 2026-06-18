package com.example.opaqueserver.domain.outbox

interface OutboxRepository {
    fun save(event: OutboxEvent)
    fun findAndClaim(limit: Int): List<OutboxEvent>
    fun delete(id: Long)
    fun unclaim(id: Long)
    fun resetStaleClaims()
}
