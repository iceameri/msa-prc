package com.example.opaqueserver.infrastructure.persistence

import com.example.opaqueserver.domain.outbox.OutboxEvent
import com.example.opaqueserver.domain.outbox.OutboxRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class OutboxJdbcRepository(private val jdbcTemplate: JdbcTemplate) : OutboxRepository {

    override fun save(event: OutboxEvent) {
        jdbcTemplate.update(
            "INSERT INTO opaque_db.public.outbox_events (topic, aggregate_key, payload) VALUES (?, ?, ?)",
            event.topic, event.aggregateKey, event.payload
        )
    }

    // 미처리(claimed_at IS NULL) 이벤트를 원자적으로 claim하고 반환.
    // FOR UPDATE SKIP LOCKED로 다중 인스턴스가 동시에 실행돼도 같은 행을 중복 처리하지 않는다.
    override fun findAndClaim(limit: Int): List<OutboxEvent> =
        jdbcTemplate.query(
            """
            UPDATE opaque_db.public.outbox_events SET claimed_at = NOW()
            WHERE id IN (
                SELECT id FROM opaque_db.public.outbox_events
                WHERE claimed_at IS NULL
                ORDER BY created_at, id
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, topic, aggregate_key, payload, claimed_at, created_at
            """.trimIndent(),
            ::mapRow, limit
        )

    override fun delete(id: Long) {
        jdbcTemplate.update(
            "DELETE FROM opaque_db.public.outbox_events WHERE id = ?", id
        )
    }

    override fun unclaim(id: Long) {
        jdbcTemplate.update(
            "UPDATE opaque_db.public.outbox_events SET claimed_at = NULL WHERE id = ?", id
        )
    }

    // claimed_at이 30초 이상 지난 이벤트를 미처리 상태로 되돌린다.
    // 릴레이 인스턴스가 claim 후 크래시된 경우 복구.
    override fun resetStaleClaims() {
        jdbcTemplate.update(
            """
            UPDATE opaque_db.public.outbox_events SET claimed_at = NULL
            WHERE claimed_at IS NOT NULL AND claimed_at < NOW() - INTERVAL '30 seconds'
            """.trimIndent()
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") n: Int) = OutboxEvent(
        id = rs.getLong("id"),
        topic = rs.getString("topic"),
        aggregateKey = rs.getString("aggregate_key"),
        payload = rs.getString("payload"),
        claimedAt = rs.getTimestamp("claimed_at")?.toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
