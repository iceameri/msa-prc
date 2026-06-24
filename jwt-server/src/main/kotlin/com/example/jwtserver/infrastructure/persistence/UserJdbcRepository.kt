package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.user.User
import com.example.jwtserver.domain.user.UserRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Repository
class UserJdbcRepository(private val jdbcTemplate: JdbcTemplate) : UserRepository {

    override fun findById(id: Long): User? =
        jdbcTemplate.query(
            "SELECT id, username, updated_at, created_at FROM jwt_db.public.authorization_users WHERE id = ?",
            ::mapRow, id
        ).firstOrNull()

    override fun findByUsername(username: String): User? =
        jdbcTemplate.query(
            "SELECT id, username, updated_at, created_at FROM jwt_db.public.authorization_users WHERE username = ?",
            ::mapRow, username
        ).firstOrNull()

    // WHERE 조건: 이벤트 발행 시각이 DB에 저장된 값보다 오래된 경우 UPDATE를 건너뜀
    // 파티션 키(userId)로 순서가 보장되지만, 파티션 수 변경 등 예외적 상황에서도 방어
    override fun sync(userId: Long, username: String, updatedAt: Instant) {
        jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.authorization_users (id, username, updated_at)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO UPDATE
                SET username   = EXCLUDED.username,
                    updated_at = EXCLUDED.updated_at
            WHERE authorization_users.updated_at < EXCLUDED.updated_at
            """.trimIndent(),
            userId, username, Timestamp.from(updatedAt)
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = User(
        id        = rs.getLong("id"),
        username  = rs.getString("username"),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
