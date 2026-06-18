package com.example.jwtserver.application.service

import com.example.jwtserver.application.port.out.OutboxRepository
import com.example.jwtserver.domain.event.OutboxEvent
import com.example.jwtserver.domain.follow.Follow
import com.example.jwtserver.domain.follow.FollowRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val userSyncService: UserSyncService,
    private val outboxRepository: OutboxRepository
) {

    @Transactional
    fun follow(followerId: Long, followerUsername: String, followingUsername: String) {
        userSyncService.sync(followerId, followerUsername)
        val followingId = userSyncService.resolveId(followingUsername)
            ?: throw NoSuchElementException("User not found: $followingUsername")
        check(followerId != followingId) { "Cannot follow yourself" }
        check(!followRepository.exists(followerId, followingId)) { "Already following" }

        followRepository.save(Follow(followerId = followerId, followingId = followingId))
        outboxRepository.save(OutboxEvent(
            aggregateId = followerId.toString(),
            aggregateType = "FOLLOW",
            eventType = "USER_FOLLOWED",
            payload = """{"actorUsername":"$followerUsername","targetUsername":"$followingUsername"}"""
        ))
    }

    @Transactional
    fun unfollow(followerId: Long, followerUsername: String, followingUsername: String) {
        userSyncService.sync(followerId, followerUsername)
        val followingId = userSyncService.resolveId(followingUsername)
            ?: throw NoSuchElementException("User not found: $followingUsername")
        check(followRepository.exists(followerId, followingId)) { "Not following" }
        followRepository.delete(followerId, followingId)
    }

    fun getFollowStats(username: String): Map<String, Int> {
        val userId = userSyncService.resolveId(username)
            ?: throw NoSuchElementException("User not found: $username")
        return mapOf(
            "followers" to followRepository.countFollowers(userId),
            "following" to followRepository.countFollowing(userId)
        )
    }
}
