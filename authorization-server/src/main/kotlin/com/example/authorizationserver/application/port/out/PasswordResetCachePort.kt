package com.example.authorizationserver.application.port.out

interface PasswordResetCachePort {
    fun saveToken(token: String, username: String)
    fun getUsernameByToken(token: String): String?
    fun deleteToken(token: String)
}
