package com.example.jwtserver.infrastructure.persistence

import com.example.jwtserver.domain.comment.Comment
import com.example.jwtserver.domain.comment.CommentRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Types

@Repository
class CommentJdbcRepository(private val jdbcTemplate: JdbcTemplate) : CommentRepository {

    private val selectBase = """
        SELECT    c.*,
                  COALESCE(u.username, sc.display_name) AS author_username
        FROM      jwt_db.public.comments c
        LEFT JOIN jwt_db.public.authorization_users u ON c.author_id = u.id
        LEFT JOIN jwt_db.public.authorization_system_clients sc ON c.client_id = sc.client_id"""

    override fun findById(id: Long): Comment? =
        jdbcTemplate.query(
            "$selectBase WHERE c.id = ?",
            ::mapRow, id
        ).firstOrNull()

    override fun findByPostId(postId: Long, offset: Int, limit: Int): List<Comment> =
        jdbcTemplate.query(
            "$selectBase WHERE c.post_id = ? ORDER BY c.created_at LIMIT ? OFFSET ?",
            ::mapRow, postId, limit, offset
        )

    override fun save(comment: Comment): Comment {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                """
                INSERT INTO jwt_db.public.comments
                (post_id, author_id, client_id, content)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("id")
            ).apply {
                setLong(1, comment.postId)
                if (comment.authorId != null) setLong(2, comment.authorId) else setNull(2, Types.BIGINT)
                if (comment.clientId != null) setString(3, comment.clientId) else setNull(3, Types.VARCHAR)
                setString(4, comment.content)
            }
        }, keyHolder)
        return comment.copy(id = keyHolder.key!!.toLong())
    }

    override fun update(comment: Comment) {
        jdbcTemplate.update(
            """
            UPDATE  jwt_db.public.comments
            SET     content = ?, updated_at = NOW()
            WHERE   id = ?
            """.trimIndent(),
            comment.content, comment.id
        )
    }

    override fun delete(id: Long) {
        jdbcTemplate.update(
            """
            DELETE FROM jwt_db.public.comments
            WHERE   id = ?
            """.trimIndent(),
            id
        )
    }

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int): Comment {
        val authorIdRaw = rs.getLong("author_id")
        val authorId = if (rs.wasNull()) null else authorIdRaw
        return Comment(
            id = rs.getLong("id"),
            postId = rs.getLong("post_id"),
            authorId = authorId,
            clientId = rs.getString("client_id"),
            authorUsername = rs.getString("author_username"),
            content = rs.getString("content"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }
}
