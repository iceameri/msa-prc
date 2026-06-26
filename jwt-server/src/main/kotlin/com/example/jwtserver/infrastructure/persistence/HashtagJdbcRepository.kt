package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.hashtag.Hashtag
import com.example.jwtserver.domain.hashtag.HashtagRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class HashtagJdbcRepository(private val jdbcTemplate: JdbcTemplate) : HashtagRepository {

    override fun findByName(name: String): Hashtag? =
        jdbcTemplate.query(
            """
            SELECT  id, name
            FROM    jwt_db.public.hashtags
            WHERE   name = ?
            """.trimIndent(),
            ::mapRow, name
        ).firstOrNull()

    override fun findByPostId(postId: Long): List<Hashtag> =
        jdbcTemplate.query(
            """
            SELECT  h.id, h.name
            FROM    jwt_db.public.hashtags h
            JOIN    jwt_db.public.post_hashtags ph ON h.id = ph.hashtag_id
            WHERE   ph.post_id = ?
            """.trimIndent(),
            ::mapRow, postId
        )

    override fun findOrCreate(name: String): Hashtag {
        val lower = name.lowercase()
        return findByName(lower) ?: run {
            val keyHolder = GeneratedKeyHolder()
            jdbcTemplate.update({ con ->
                con.prepareStatement(
                    """
                    INSERT INTO jwt_db.public.hashtags (name)
                    VALUES (?) ON CONFLICT (name) DO NOTHING
                    """.trimIndent(),
                    arrayOf("id")
                ).apply { setString(1, lower) }
            }, keyHolder)
            findByName(lower)!!
        }
    }

    override fun linkToPost(postId: Long, hashtagId: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO jwt_db.public.post_hashtags (post_id, hashtag_id)
            VALUES (?, ?) ON CONFLICT DO NOTHING
            """.trimIndent(),
            postId, hashtagId
        )
    }

    override fun unlinkFromPost(postId: Long) {
        jdbcTemplate.update(
            """
            DELETE FROM jwt_db.public.post_hashtags
            WHERE   post_id = ?
            """.trimIndent(),
            postId
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) =
        Hashtag(id = rs.getLong("id"), name = rs.getString("name"))
}
