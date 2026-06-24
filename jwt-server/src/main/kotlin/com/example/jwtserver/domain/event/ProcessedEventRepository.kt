package com.example.jwtserver.domain.event

interface ProcessedEventRepository {
    fun insertIfAbsent(eventId: String, topic: String): Boolean
    fun deleteOlderThan(days: Long)
}
