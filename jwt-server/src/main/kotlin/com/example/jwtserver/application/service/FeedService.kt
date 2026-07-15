package com.example.jwtserver.application.service

import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.infrastructure.persistence.FollowJdbcRepository
import com.example.jwtserver.infrastructure.persistence.PostJdbcRepository
import org.springframework.stereotype.Service

@Service
class FeedService(
    private val postRepository: PostJdbcRepository,
    private val followRepository: FollowJdbcRepository,
    private val userSyncService: UserSyncService
) {

    fun getFeed(userId: Long, username: String, offset: Int, limit: Int): List<Post> {
        userSyncService.sync(userId, username)
        val followingIds = followRepository.findFollowingIds(userId)
        if (followingIds.isEmpty()) return emptyList()
        return postRepository.findFeedPosts(followingIds, offset, limit)
    }
}
