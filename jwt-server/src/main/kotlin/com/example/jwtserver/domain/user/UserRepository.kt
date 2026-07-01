package com.example.jwtserver.domain.user

interface UserRepository {
    fun findById(id: Long): User?
    fun findByUsername(username: String): User?
    fun sync(userId: Long, username: String, enabled: Boolean, status: String, version: Long)
    fun syncUsername(userId: Long, newUsername: String, version: Long)
}
