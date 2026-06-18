package com.example.jwtserver.application.service

import com.example.jwtserver.domain.follow.FollowRepository
import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.domain.post.PostRepository
import org.springframework.stereotype.Service

@Service
class FeedService(
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository,
    private val userSyncService: UserSyncService
) {

    fun getFeed(userId: Long, username: String, offset: Int, limit: Int): List<Post> {
        userSyncService.sync(userId, username)
        val followingIds = followRepository.findFollowingIds(userId)
        if (followingIds.isEmpty()) return emptyList()
        return postRepository.findFeedPosts(followingIds, offset, limit)
    }
}
