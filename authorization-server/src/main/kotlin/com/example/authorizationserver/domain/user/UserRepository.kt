package com.example.authorizationserver.domain.user

import java.time.Instant

interface UserRepository {
    fun findByUsername(username: String): User?
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun lockUser(username: String, lockedUntil: Instant)
    fun resetLoginAttempts(username: String)
    fun updateMfaSettings(username: String, mfaEnabled: Boolean, mfaSecret: String?)
    fun setStatusAndEnabled(userId: Long, enabled: Boolean, status: String)
    fun updateUsername(userId: Long, newUsername: String)
}
