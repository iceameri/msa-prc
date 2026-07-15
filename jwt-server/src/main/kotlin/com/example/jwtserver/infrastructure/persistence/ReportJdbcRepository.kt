package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.report.Report
import com.example.jwtserver.domain.report.ReportTargetType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

@Repository
class ReportJdbcRepository(private val jdbcTemplate: JdbcTemplate) {

    fun save(report: Report): Report {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement("""
                INSERT INTO jwt_db.public.reports (reporter_id, target_type, target_id, reason)
                VALUES (?, ?, ?, ?)
                """.trimMargin(), arrayOf("id")
            ).apply {
                setLong(1, report.reporterId)
                setString(2, report.targetType.name)
                setLong(3, report.targetId)
                setString(4, report.reason)
            }
        }, keyHolder)
        return report.copy(id = keyHolder.key!!.toLong())
    }
}
