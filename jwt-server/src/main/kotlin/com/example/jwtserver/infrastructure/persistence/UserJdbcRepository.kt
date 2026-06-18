package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.user.User
import com.example.jwtserver.domain.user.UserRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class UserJdbcRepository(private val jdbcTemplate: JdbcTemplate) : UserRepository {

    override fun findById(id: Long): User? =
        jdbcTemplate.query("SELECT id, username, created_at FROM jwt_db.public.authorization_users WHERE id = ?", ::mapRow, id)
            .firstOrNull()

    override fun findByUsername(username: String): User? =
        jdbcTemplate.query("SELECT id, username, created_at FROM jwt_db.public.authorization_users WHERE username = ?", ::mapRow, username)
            .firstOrNull()

    override fun sync(userId: Long, username: String) {
        jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.authorization_users (id, username)
            VALUES (?, ?)
            ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username
            """.trimIndent(),
            userId, username
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = User(
        id = rs.getLong("id"),
        username = rs.getString("username"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
