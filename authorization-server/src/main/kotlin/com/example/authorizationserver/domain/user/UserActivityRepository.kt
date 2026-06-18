package com.example.authorizationserver.domain.user

interface UserActivityRepository {
    fun upsert(userId: Long)
}
