package com.example.jwtserver.domain.comment

interface CommentRepository {
    fun findById(id: Long): Comment?
    fun findByPostId(postId: Long, offset: Int, limit: Int): List<Comment>
    fun save(comment: Comment): Comment
    fun update(comment: Comment)
    fun delete(id: Long)
}
