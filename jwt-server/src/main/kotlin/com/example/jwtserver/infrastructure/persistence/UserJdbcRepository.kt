package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.user.User
import com.example.jwtserver.domain.user.UserRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class UserJdbcRepository(private val jdbcTemplate: JdbcTemplate) : UserRepository {

    override fun findById(id: Long): User? =
        jdbcTemplate.query(
            """
            SELECT  user_id, username, enabled, status, version, created_at
            FROM    jwt_db.public.authorization_users
            WHERE   user_id = ?
            """.trimIndent(),
            ::mapRow, id
        ).firstOrNull()

    override fun findByUsername(username: String): User? =
        jdbcTemplate.query(
            """
            SELECT  user_id, username, enabled, status, version, created_at
            FROM    jwt_db.public.authorization_users
            WHERE   username = ?
            """.trimIndent(),
            ::mapRow, username
        ).firstOrNull()

    override fun sync(userId: Long, username: String, enabled: Boolean, status: String, version: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.authorization_users (user_id, username, enabled, status, version)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (user_id) DO UPDATE
                SET username   = EXCLUDED.username,
                    enabled    = EXCLUDED.enabled,
                    status     = EXCLUDED.status,
                    version    = EXCLUDED.version,
                    updated_at = NOW()
            WHERE authorization_users.version < EXCLUDED.version
            """.trimIndent(),
            userId, username, enabled, status, version
        )
    }

    // username 변경 전용 — enabled/status는 현재 값 유지
    override fun syncUsername(userId: Long, newUsername: String, version: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.authorization_users (user_id, username, enabled, status, version)
            VALUES (?, ?, TRUE, 'ACTIVE', ?)
            ON CONFLICT (user_id) DO UPDATE
                SET username   = EXCLUDED.username,
                    version    = EXCLUDED.version,
                    updated_at = NOW()
            WHERE authorization_users.version < EXCLUDED.version
            """.trimIndent(),
            userId, newUsername, version
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = User(
        id        = rs.getLong("user_id"),
        username  = rs.getString("username"),
        enabled   = rs.getBoolean("enabled"),
        status    = rs.getString("status"),
        version   = rs.getLong("version"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
