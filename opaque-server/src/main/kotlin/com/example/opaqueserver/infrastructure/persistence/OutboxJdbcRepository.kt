package com.example.opaqueserver.infrastructure.persistence

import com.example.opaqueserver.domain.outbox.OutboxEvent
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class OutboxJdbcRepository(private val jdbcTemplate: JdbcTemplate) {

    fun save(event: OutboxEvent) {
        jdbcTemplate.update(
            """
            INSERT INTO opaque_db.public.outbox_events (aggregate_id, aggregate_type, event_type, payload)
            VALUES (?, ?, ?, ?::jsonb)
            """.trimMargin(),
            event.aggregateId, event.aggregateType, event.eventType, event.payload
        )
    }

    // 미처리(claimed_at IS NULL AND sent_at IS NULL) 이벤트를 원자적으로 claim하고 반환.
    // FOR UPDATE SKIP LOCKED로 다중 인스턴스가 동시에 실행돼도 같은 행을 중복 처리하지 않는다.
    fun findAndClaim(limit: Int): List<OutboxEvent> =
        jdbcTemplate.query(
            """
            UPDATE opaque_db.public.outbox_events SET claimed_at = NOW()
            WHERE id IN (
                SELECT  id
                FROM    opaque_db.public.outbox_events
                WHERE   claimed_at IS NULL AND sent_at IS NULL
                ORDER BY created_at, id
                LIMIT   ?
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, aggregate_id, aggregate_type, event_type, payload::text, claimed_at, sent_at, created_at
            """.trimMargin(),
            ::mapRow, limit
        )

    fun markSent(id: Long) {
        jdbcTemplate.update(
            """
            UPDATE  opaque_db.public.outbox_events
            SET     sent_at = NOW()
            WHERE   id = ?
            """.trimMargin(),
            id
        )
    }

    fun unclaim(id: Long) {
        jdbcTemplate.update(
            """
            UPDATE  opaque_db.public.outbox_events
            SET     claimed_at = NULL
            WHERE   id = ?
            """.trimMargin(),
            id
        )
    }

    // claimed_at이 30초 이상 지난 미전송 이벤트를 미처리 상태로 되돌린다.
    // 릴레이 인스턴스가 claim 후 크래시된 경우 복구.
    fun resetStaleClaims() {
        jdbcTemplate.update(
            """
            UPDATE  opaque_db.public.outbox_events
            SET     claimed_at = NULL
            WHERE   claimed_at IS NOT NULL AND
                    sent_at IS NULL AND
                    claimed_at < NOW() - INTERVAL '30 seconds'
            """.trimMargin()
        )
    }

    // 7일 이상 지난 전송 완료 이벤트를 삭제한다.
    fun deleteProcessed() {
        jdbcTemplate.update(
            """
            DELETE FROM opaque_db.public.outbox_events
            WHERE   sent_at IS NOT NULL AND
                    sent_at < NOW() - INTERVAL '7 days'
            """.trimMargin()
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") n: Int) = OutboxEvent(
        id = rs.getLong("id"),
        aggregateId = rs.getString("aggregate_id"),
        aggregateType = rs.getString("aggregate_type"),
        eventType = rs.getString("event_type"),
        payload = rs.getString("payload"),
        claimedAt = rs.getTimestamp("claimed_at")?.toInstant(),
        sentAt = rs.getTimestamp("sent_at")?.toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
