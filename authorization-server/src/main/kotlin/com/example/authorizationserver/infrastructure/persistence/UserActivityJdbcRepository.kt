package com.example.authorizationserver.infrastructure.persistence

import com.example.authorizationserver.domain.user.UserActivityRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class UserActivityJdbcRepository(private val jdbcTemplate: JdbcTemplate) : UserActivityRepository {

    override fun upsert(userId: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO authorization_db.public.user_activity (user_id, last_active_at)
            VALUES (?, NOW())
            ON CONFLICT (user_id) DO UPDATE SET last_active_at = EXCLUDED.last_active_at
            """.trimIndent(),
            userId
        )
    }
}
