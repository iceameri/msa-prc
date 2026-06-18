package com.example.jwtserver.application.port.out

import com.example.jwtserver.domain.post.Post

interface SearchPort {
    fun indexPost(post: Post, hashtags: List<String>)
    fun deletePost(postId: Long)
    fun searchPosts(keyword: String, offset: Int, limit: Int): List<Long>
    fun searchUsers(keyword: String, offset: Int, limit: Int): List<String>
}
