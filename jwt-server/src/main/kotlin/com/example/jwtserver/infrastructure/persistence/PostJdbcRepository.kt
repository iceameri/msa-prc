package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.domain.post.PostRepository
import com.example.jwtserver.domain.post.PostStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Types

@Repository
class PostJdbcRepository(private val jdbcTemplate: JdbcTemplate) : PostRepository {

    private val selectBase = """
        SELECT    p.*,
                  COALESCE(u.username, sc.display_name) AS author_username
        FROM      jwt_db.public.posts p
        LEFT JOIN jwt_db.public.authorization_users u ON p.author_id = u.user_id
        LEFT JOIN jwt_db.public.authorization_system_clients sc ON p.client_id = sc.client_id"""

    override fun findById(id: Long): Post? =
        jdbcTemplate.query(
            """
            $selectBase
            WHERE p.id = ?
            AND p.status = 'ACTIVE'
            """.trimMargin(),
            ::mapRow, id
        ).firstOrNull()

    override fun findAll(offset: Int, limit: Int): List<Post> =
        jdbcTemplate.query(
            """
            $selectBase
            WHERE p.status = 'ACTIVE'
            ORDER BY p.id
            LIMIT ? OFFSET ?
            """.trimMargin(),
            ::mapRow, limit, offset
        )

    override fun findByAuthorId(authorId: Long, offset: Int, limit: Int): List<Post> =
        jdbcTemplate.query(
            """
            $selectBase
            WHERE p.author_id = ?
            AND p.status = 'ACTIVE'
            ORDER BY p.created_at DESC
            LIMIT ? OFFSET ?
            """.trimMargin(),
            ::mapRow, authorId, limit, offset
        )

    override fun findFeedPosts(followingIds: List<Long>, offset: Int, limit: Int): List<Post> {
        val placeholders = followingIds.joinToString(",") { "?" }
        val args: Array<Any> = (followingIds.map { it as Any } + limit + offset).toTypedArray()
        return jdbcTemplate.query(
            """
            $selectBase
            WHERE   p.author_id IN ($placeholders)
            AND     p.status = 'ACTIVE'
            ORDER BY p.created_at DESC LIMIT ? OFFSET ?
            """.trimMargin(),
            ::mapRow, *args
        )
    }

    override fun findByHashtag(hashtagName: String, offset: Int, limit: Int): List<Post> =
        jdbcTemplate.query(
            """
            $selectBase
            JOIN    jwt_db.public.post_hashtags ph ON p.id = ph.post_id
            JOIN    jwt_db.public.hashtags h       ON ph.hashtag_id = h.id
            WHERE   h.name = ? AND p.status = 'ACTIVE'
            ORDER BY p.created_at DESC LIMIT ? OFFSET ?
            """.trimMargin(),
            ::mapRow, hashtagName, limit, offset
        )

    override fun save(post: Post): Post {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                """
                INSERT INTO jwt_db.public.posts (author_id, client_id, title, content, image_url)
                VALUES (?, ?, ?, ?, ?)
                """.trimMargin(),
                arrayOf("id")
            ).apply {
                if (post.authorId != null) setLong(1, post.authorId) else setNull(1, Types.BIGINT)
                if (post.clientId != null) setString(2, post.clientId) else setNull(2, Types.VARCHAR)
                setString(3, post.title)
                setString(4, post.content)
                setString(5, post.imageUrl)
            }
        }, keyHolder)
        return post.copy(id = keyHolder.key!!.toLong())
    }

    override fun update(post: Post) {
        jdbcTemplate.update(
            """
            UPDATE  jwt_db.public.posts
            SET     title = ?, content = ?, image_url = ?, updated_at = NOW()
            WHERE   id = ?
            """.trimMargin(),
            post.title, post.content, post.imageUrl, post.id
        )
    }

    override fun delete(id: Long) {
        jdbcTemplate.update(
            """
            UPDATE  jwt_db.public.posts
            SET     status = 'DELETED', updated_at = NOW()
            WHERE   id = ?
            """.trimMargin(),
            id
        )
    }

    override fun incrementLikeCount(id: Long) {
        jdbcTemplate.update(
            """
            UPDATE  jwt_db.public.posts
            SET     like_count = like_count + 1
            WHERE   id = ?
            """.trimMargin(),
            id
        )
    }

    override fun decrementLikeCount(id: Long) {
        jdbcTemplate.update(
            """
            UPDATE  jwt_db.public.posts
            SET     like_count = GREATEST(0, like_count - 1)
            WHERE   id = ?
            """.trimMargin(),
            id
        )
    }

    override fun incrementCommentCount(id: Long) {
        jdbcTemplate.update(
            """
            UPDATE  jwt_db.public.posts
            SET     comment_count = comment_count + 1
            WHERE   id = ?
            """.trimMargin(),
            id
        )
    }

    override fun decrementCommentCount(id: Long) {
        jdbcTemplate.update(
            """
            UPDATE  jwt_db.public.posts
            SET     comment_count = GREATEST(0, comment_count - 1)
            WHERE   id = ?
            """.trimMargin(),
            id
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int): Post {
        val authorIdRaw = rs.getLong("author_id")
        val authorId = if (rs.wasNull()) null else authorIdRaw
        return Post(
            id = rs.getLong("id"),
            authorId = authorId,
            clientId = rs.getString("client_id"),
            authorUsername = rs.getString("author_username"),
            title = rs.getString("title"),
            content = rs.getString("content"),
            imageUrl = rs.getString("image_url"),
            likeCount = rs.getInt("like_count"),
            commentCount = rs.getInt("comment_count"),
            status = PostStatus.valueOf(rs.getString("status")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }
}
