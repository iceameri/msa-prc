package com.example.jwtserver.domain.like

interface LikeRepository {
    fun exists(postId: Long, userId: Long): Boolean
    fun save(like: Like)
    fun delete(postId: Long, userId: Long)
}
