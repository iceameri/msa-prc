package com.example.jwtserver.application.service

import com.example.jwtserver.application.port.out.SearchPort
import com.example.jwtserver.domain.hashtag.HashtagRepository
import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.domain.post.PostRepository
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val searchPort: SearchPort,
    private val postRepository: PostRepository,
    private val hashtagRepository: HashtagRepository
) {

    fun searchPosts(keyword: String, offset: Int, limit: Int): List<Post> {
        val postIds = searchPort.searchPosts(keyword, offset, limit)
        return postIds.mapNotNull { postRepository.findById(it) }
    }

    fun searchUsers(keyword: String, offset: Int, limit: Int): List<String> =
        searchPort.searchUsers(keyword, offset, limit)

    fun reindexAll(): Int {
        val batchSize = 500
        var offset = 0
        var total = 0
        while (true) {
            val posts = postRepository.findAll(offset, batchSize)
            if (posts.isEmpty()) break
            posts.forEach { post ->
                val hashtags = hashtagRepository.findByPostId(post.id!!).map { it.name }
                searchPort.indexPost(post, hashtags)
            }
            total += posts.size
            offset += batchSize
        }
        return total
    }
}
