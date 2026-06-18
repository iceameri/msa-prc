package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.application.port.out.OutboxRepository
import com.example.jwtserver.domain.event.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class OutboxJdbcRepository(private val jdbcTemplate: JdbcTemplate) : OutboxRepository {

    override fun save(event: OutboxEvent) {
        jdbcTemplate.update(
            """
                INSERT INTO jwt_db.public.outbox_events 
                (aggregate_id, aggregate_type, event_type, payload)
                VALUES (?, ?, ?, ?::jsonb)""",
            event.aggregateId, event.aggregateType, event.eventType, event.payload
        )
    }

    override fun findUnpublished(limit: Int): List<OutboxEvent> =
        jdbcTemplate.query(
            """
                SELECT  id,
                        aggregate_id,
                        aggregate_type,
                        event_type,
                        payload::text,
                        published,
                        created_at
                FROM    jwt_db.public.outbox_events
                WHERE   published = false
                ORDER BY created_at ASC LIMIT ?""",
            ::mapRow, limit
        )

    override fun markPublished(id: Long) {
        jdbcTemplate.update(
            "UPDATE jwt_db.public.outbox_events SET published = true WHERE id = ?", id
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = OutboxEvent(
        id = rs.getLong("id"),
        aggregateId = rs.getString("aggregate_id"),
        aggregateType = rs.getString("aggregate_type"),
        eventType = rs.getString("event_type"),
        payload = rs.getString("payload"),
        published = rs.getBoolean("published"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
