package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.follow.Follow
import com.example.jwtserver.domain.follow.FollowRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository

@Repository
class FollowJdbcRepository(private val jdbcTemplate: JdbcTemplate) : FollowRepository {

    override fun exists(followerId: Long, followingId: Long): Boolean =
        (jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    jwt_db.public.follows
            WHERE   follower_id = ? AND following_id = ?
            """.trimIndent(),
            followerId, followingId
        ) ?: 0) > 0

    override fun save(follow: Follow) {
        jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.follows (follower_id, following_id)
            VALUES (?, ?) ON CONFLICT DO NOTHING
            """.trimIndent(),
            follow.followerId, follow.followingId
        )
    }

    override fun delete(followerId: Long, followingId: Long) {
        jdbcTemplate.update(
            """
            DELETE FROM jwt_db.public.follows
            WHERE   follower_id = ? AND following_id = ?
            """.trimIndent(),
            followerId, followingId
        )
    }

    override fun findFollowingIds(followerId: Long): List<Long> =
        jdbcTemplate.queryForList<Long>(
            """
            SELECT  following_id
            FROM    jwt_db.public.follows
            WHERE   follower_id = ?
            """.trimIndent(),
            followerId
        ).filterNotNull()

    override fun countFollowers(userId: Long): Int =
        jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    jwt_db.public.follows
            WHERE   following_id = ?
            """.trimIndent(),
            userId
        ) ?: 0

    override fun countFollowing(userId: Long): Int =
        jdbcTemplate.queryForObject<Int>(
            """
            SELECT  COUNT(*)
            FROM    jwt_db.public.follows
            WHERE   follower_id = ?
            """.trimIndent(),
            userId
        ) ?: 0
}
