package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.follow.Follow
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository

@Repository
class FollowJdbcRepository(private val jdbcTemplate: JdbcTemplate) {

    fun exists(followerId: Long, followingId: Long): Boolean =
        (jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    jwt_db.public.follows
            WHERE   follower_id = ? AND following_id = ?
            """.trimMargin(),
            followerId, followingId
        ) ?: 0) > 0

    fun save(follow: Follow) {
        jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.follows (follower_id, following_id)
            VALUES (?, ?) ON CONFLICT DO NOTHING
            """.trimMargin(),
            follow.followerId, follow.followingId
        )
    }

    fun delete(followerId: Long, followingId: Long) {
        jdbcTemplate.update(
            """
            DELETE FROM jwt_db.public.follows
            WHERE   follower_id = ? AND following_id = ?
            """.trimMargin(),
            followerId, followingId
        )
    }

    fun findFollowingIds(followerId: Long): List<Long> =
        jdbcTemplate.queryForList<Long>(
            """
            SELECT  following_id
            FROM    jwt_db.public.follows
            WHERE   follower_id = ?
            """.trimMargin(),
            followerId
        ).filterNotNull()

    fun countFollowers(userId: Long): Int =
        jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    jwt_db.public.follows
            WHERE   following_id = ?
            """.trimMargin(),
            userId
        ) ?: 0

    fun countFollowing(userId: Long): Int =
        jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    jwt_db.public.follows
            WHERE   follower_id = ?
            """.trimMargin(),
            userId
        ) ?: 0
}
