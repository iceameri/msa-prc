package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.comment.Comment
import com.example.jwtserver.domain.comment.CommentRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class CommentJdbcRepository(private val jdbcTemplate: JdbcTemplate) : CommentRepository {

    override fun findById(id: Long): Comment? =
        jdbcTemplate.query(
            """
                SELECT       c.*, 
                             u.username AS author_username
                FROM         jwt_db.public.comments c
                LEFT JOIN    jwt_db.public.authorization_users u ON c.author_id = u.id
                WHERE        c.id = ?""",
            ::mapRow, id
        ).firstOrNull()

    override fun findByPostId(postId: Long, offset: Int, limit: Int): List<Comment> =
        jdbcTemplate.query(
            """
                SELECT       c.*, 
                             u.username AS author_username
                FROM         jwt_db.public.comments c
                LEFT JOIN    jwt_db.public.authorization_users u ON c.author_id = u.id
                WHERE        c.post_id = ?
                ORDER BY     c.created_at LIMIT ? OFFSET ?""",
            ::mapRow, postId, limit, offset
        )

    override fun save(comment: Comment): Comment {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                "INSERT INTO jwt_db.public.comments (post_id, author_id, content) VALUES (?, ?, ?)", arrayOf("id")
            ).apply {
                setLong(1, comment.postId)
                setLong(2, comment.authorId)
                setString(3, comment.content)
            }
        }, keyHolder)
        return comment.copy(id = keyHolder.key!!.toLong())
    }

    override fun update(comment: Comment) {
        jdbcTemplate.update(
            "UPDATE jwt_db.public.comments SET content = ?, updated_at = NOW() WHERE id = ?",
            comment.content, comment.id
        )
    }

    override fun delete(id: Long) {
        jdbcTemplate.update("DELETE FROM jwt_db.public.comments WHERE id = ?", id)
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = Comment(
        id = rs.getLong("id"),
        postId = rs.getLong("post_id"),
        authorId = rs.getLong("author_id"),
        authorUsername = rs.getString("author_username"),
        content = rs.getString("content"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
