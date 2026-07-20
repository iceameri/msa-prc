package com.example.authorizationserver.application.port.out

import com.example.authorizationserver.domain.user.User

interface UserCachePort {
    fun getUser(username: String): User?
    fun getUser(username: String, tenantId: Long): User?
    fun saveUser(user: User)
    fun deleteUser(username: String)
    fun deleteUser(username: String, tenantId: Long)
    fun deleteAuthorities(userId: Long)
    fun getLoginAttempts(username: String): Int
    fun incrementLoginAttempts(username: String): Int
    fun resetLoginAttempts(username: String)
    fun saveAuthorities(userId: Long, authorities: Set<String?>)
    fun getAuthorities(userId: Long): Set<String>?
    fun savePendingMfaSecret(username: String, secret: String)
    fun getPendingMfaSecret(username: String): String?
    fun deletePendingMfaSecret(username: String)
}
