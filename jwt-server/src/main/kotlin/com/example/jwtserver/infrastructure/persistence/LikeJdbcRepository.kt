package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.like.Like
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository

@Repository
class LikeJdbcRepository(private val jdbcTemplate: JdbcTemplate) {

    fun exists(postId: Long, userId: Long): Boolean =
        (jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    jwt_db.public.likes
            WHERE   post_id = ? AND user_id = ?
            """.trimMargin(),
            postId, userId
        ) ?: 0) > 0

    fun save(like: Like) {
        jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.likes (post_id, user_id)
            VALUES (?, ?) ON CONFLICT DO NOTHING
            """.trimMargin(),
            like.postId, like.userId
        )
    }

    fun delete(postId: Long, userId: Long) {
        jdbcTemplate.update("""
            DELETE FROM jwt_db.public.likes
            WHERE post_id = ? AND user_id = ?
            """.trimMargin(),
            postId, userId)
    }
}
