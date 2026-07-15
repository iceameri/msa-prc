package com.example.opaqueserver.infrastructure.persistence

import com.example.opaqueserver.domain.report.Report
import com.example.opaqueserver.domain.report.ReportStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class ReportJdbcRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findById(id: Long): Report? =
        jdbcTemplate.query("SELECT * FROM opaque_db.public.reports WHERE id = ?", ::mapRow, id).firstOrNull()

    fun findByStatus(status: ReportStatus, offset: Int, limit: Int): List<Report> =
        jdbcTemplate.query(
            """
            SELECT  id,
                    external_id,
                    reporter_username,
                    target_type,
                    target_id,
                    reason,
                    status,
                    reviewed_by,
                    reviewed_at,
                    created_at
            FROM    opaque_db.public.reports
            WHERE   status = ?
            ORDER BY created_at
            LIMIT ? OFFSET ?
            """.trimMargin(),
            ::mapRow, status.name, limit, offset
        )

    fun existsByExternalId(externalId: Long): Boolean =
        (jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    opaque_db.public.reports
            WHERE   external_id = ?
            """.trimMargin(),
            externalId
        ) ?: 0) > 0

    fun save(report: Report): Report {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                """
                INSERT INTO opaque_db.public.reports (external_id, reporter_username, target_type, target_id, reason)
                VALUES (?, ?, ?, ?, ?)
                """.trimMargin(),
                arrayOf("id")
            ).apply {
                setLong(1, report.externalId)
                setString(2, report.reporterUsername)
                setString(3, report.targetType)
                setLong(4, report.targetId)
                setString(5, report.reason)
            }
        }, keyHolder)
        return report.copy(id = keyHolder.key!!.toLong())
    }

    fun updateStatus(id: Long, status: ReportStatus, reviewedBy: String) {
        jdbcTemplate.update(
            """
            UPDATE  opaque_db.public.reports
            SET     status = ?,
                    reviewed_by = ?,
                    reviewed_at = NOW()
            WHERE   id = ?
            """.trimMargin(),
            status.name, reviewedBy, id
        )
    }

    fun countByStatus(status: ReportStatus): Int =
        jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    opaque_db.public.reports
            WHERE   status = ?
            """.trimMargin(),
            status.name
        ) ?: 0

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") n: Int) = Report(
        id = rs.getLong("id"),
        externalId = rs.getLong("external_id"),
        reporterUsername = rs.getString("reporter_username"),
        targetType = rs.getString("target_type"),
        targetId = rs.getLong("target_id"),
        reason = rs.getString("reason"),
        status = ReportStatus.valueOf(rs.getString("status")),
        reviewedBy = rs.getString("reviewed_by"),
        reviewedAt = rs.getTimestamp("reviewed_at")?.toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
