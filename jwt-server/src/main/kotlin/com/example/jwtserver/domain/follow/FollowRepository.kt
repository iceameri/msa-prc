package com.example.jwtserver.domain.follow

interface FollowRepository {
    fun exists(followerId: Long, followingId: Long): Boolean
    fun save(follow: Follow)
    fun delete(followerId: Long, followingId: Long)
    fun findFollowingIds(followerId: Long): List<Long>
    fun countFollowers(userId: Long): Int
    fun countFollowing(userId: Long): Int
}
