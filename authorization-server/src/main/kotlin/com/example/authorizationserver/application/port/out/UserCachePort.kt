package com.example.authorizationserver.application.port.out

import com.example.authorizationserver.domain.user.User

interface UserCachePort {
    fun getUser(username: String): User?
    fun saveUser(user: User)
    fun deleteUser(username: String)
    fun deleteAuthorities(username: String)
    fun getLoginAttempts(username: String): Int
    fun incrementLoginAttempts(username: String): Int
    fun resetLoginAttempts(username: String)
    fun saveAuthorities(username: String, authorities: Set<String?>)
    fun getAuthorities(username: String): Set<String>?
    fun savePendingMfaSecret(username: String, secret: String)
    fun getPendingMfaSecret(username: String): String?
    fun deletePendingMfaSecret(username: String)
}
