package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.domain.post.PostRepository
import com.example.jwtserver.domain.post.PostStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class PostJdbcRepository(private val jdbcTemplate: JdbcTemplate) : PostRepository {

    override fun findById(id: Long): Post? =
        jdbcTemplate.query(
            """
                SELECT       p.*,
                             u.username AS author_username
                FROM         jwt_db.public.posts p
                LEFT JOIN    jwt_db.public.authorization_users u ON p.author_id = u.id
                WHERE        p.id = ? AND p.status = 'ACTIVE'""",
            ::mapRow, id
        ).firstOrNull()

    override fun findAll(offset: Int, limit: Int): List<Post> =
        jdbcTemplate.query(
            """
                SELECT       p.*, u.username AS author_username
                FROM         jwt_db.public.posts p
                LEFT JOIN    jwt_db.public.authorization_users u ON p.author_id = u.id
                WHERE        p.status = 'ACTIVE'
                ORDER BY     p.id ASC LIMIT ? OFFSET ?""",
            ::mapRow, limit, offset
        )

    override fun findByAuthorId(authorId: Long, offset: Int, limit: Int): List<Post> =
        jdbcTemplate.query(
            """
                SELECT       p.*, u.username AS author_username
                FROM         jwt_db.public.posts p
                LEFT JOIN    jwt_db.public.authorization_users u ON p.author_id = u.id
                WHERE        p.author_id = ? AND p.status = 'ACTIVE'
                ORDER BY     p.created_at DESC LIMIT ? OFFSET ?""",
            ::mapRow, authorId, limit, offset
        )

    override fun findFeedPosts(followingIds: List<Long>, offset: Int, limit: Int): List<Post> {
        val placeholders = followingIds.joinToString(",") { "?" }
        val args: Array<Any> = (followingIds.map { it as Any } + limit + offset).toTypedArray()
        return jdbcTemplate.query(
            """
                SELECT    p.*,
                          u.username AS author_username
                FROM      jwt_db.public.posts p
                LEFT JOIN jwt_db.public.authorization_users u ON p.author_id = u.id
                WHERE     p.author_id IN ($placeholders) AND p.status = 'ACTIVE'
                ORDER BY  p.created_at DESC LIMIT ? OFFSET ?""",
            ::mapRow, *args
        )
    }

    override fun findByHashtag(hashtagName: String, offset: Int, limit: Int): List<Post> =
        jdbcTemplate.query(
            """
                SELECT    p.*, u.username AS author_username
                FROM      jwt_db.public.posts p
                LEFT JOIN jwt_db.public.authorization_users u ON p.author_id = u.id
                JOIN      jwt_db.public.post_hashtags ph ON p.id = ph.post_id
                JOIN      jwt_db.public.hashtags h ON ph.hashtag_id = h.id
                WHERE     h.name = ? AND p.status = 'ACTIVE'
                ORDER BY  p.created_at DESC LIMIT ? OFFSET ?""",
            ::mapRow, hashtagName, limit, offset
        )

    override fun save(post: Post): Post {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                "INSERT INTO jwt_db.public.posts (author_id, title, content, image_url) VALUES (?, ?, ?, ?)", arrayOf("id")
            ).apply {
                setLong(1, post.authorId)
                setString(2, post.title)
                setString(3, post.content)
                setString(4, post.imageUrl)
            }
        }, keyHolder)
        return post.copy(id = keyHolder.key!!.toLong())
    }

    override fun update(post: Post) {
        jdbcTemplate.update(
            "UPDATE jwt_db.public.posts SET title = ?, content = ?, image_url = ?, updated_at = NOW() WHERE id = ?",
            post.title, post.content, post.imageUrl, post.id
        )
    }

    override fun delete(id: Long) {
        jdbcTemplate.update("UPDATE jwt_db.public.posts SET status = 'DELETED', updated_at = NOW() WHERE id = ?", id)
    }

    override fun incrementLikeCount(id: Long) {
        jdbcTemplate.update("UPDATE jwt_db.public.posts SET like_count = like_count + 1 WHERE id = ?", id)
    }

    override fun decrementLikeCount(id: Long) {
        jdbcTemplate.update("UPDATE jwt_db.public.posts SET like_count = GREATEST(0, like_count - 1) WHERE id = ?", id)
    }

    override fun incrementCommentCount(id: Long) {
        jdbcTemplate.update("UPDATE jwt_db.public.posts SET comment_count = comment_count + 1 WHERE id = ?", id)
    }

    override fun decrementCommentCount(id: Long) {
        jdbcTemplate.update("UPDATE jwt_db.public.posts SET comment_count = GREATEST(0, comment_count - 1) WHERE id = ?", id)
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = Post(
        id = rs.getLong("id"),
        authorId = rs.getLong("author_id"),
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
