package com.example.opaqueserver.infrastructure.persistence

import com.example.opaqueserver.domain.audit.AuditLog
import com.example.opaqueserver.domain.audit.AuditRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class AuditJdbcRepository(private val jdbcTemplate: JdbcTemplate) : AuditRepository {

    override fun save(log: AuditLog) {
        jdbcTemplate.update(
            """
            INSERT INTO opaque_db.public.audit_logs
            (actor_id, actor_username, action, target_type, target_id, detail)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimMargin(),
            log.actorId, log.actorUsername, log.action, log.targetType, log.targetId, log.detail
        )
    }

    override fun findAll(offset: Int, limit: Int): List<AuditLog> =
        jdbcTemplate.query(
            """
            SELECT  id,
                    actor_id,
                    actor_username,
                    action,
                    target_type,
                    target_id,
                    detail,
                    created_at
            FROM    opaque_db.public.audit_logs
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """.trimMargin(),
            ::mapRow, limit, offset
        )

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") n: Int) = AuditLog(
        id = rs.getLong("id"),
        actorId = rs.getString("actor_id"),
        actorUsername = rs.getString("actor_username"),
        action = rs.getString("action"),
        targetType = rs.getString("target_type"),
        targetId = rs.getString("target_id"),
        detail = rs.getString("detail"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
