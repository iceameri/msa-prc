package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.event.ProcessedEventRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProcessedEventJdbcRepository(private val jdbcTemplate: JdbcTemplate) : ProcessedEventRepository {

    override fun insertIfAbsent(eventId: String, topic: String): Boolean {
        val affected = jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.processed_kafka_events (event_id, topic)
            VALUES (?, ?) ON CONFLICT (event_id) DO NOTHING
            """.trimMargin(),
            eventId, topic
        )
        return affected > 0
    }

    override fun deleteOlderThan(days: Long) {
        jdbcTemplate.update(
            """
            DELETE FROM jwt_db.public.processed_kafka_events
            WHERE   processed_at < NOW() - (? || ' days')::INTERVAL
            """.trimMargin(),
            days.toString()
        )
    }
}
