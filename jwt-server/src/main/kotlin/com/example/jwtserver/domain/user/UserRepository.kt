package com.example.jwtserver.domain.user

import java.time.Instant

interface UserRepository {
    fun findById(id: Long): User?
    fun findByUsername(username: String): User?
    // updatedAt: 이벤트 발행 시각 — DB의 updated_at보다 오래된 경우 무시됨
    fun sync(userId: Long, username: String, updatedAt: Instant)
}
