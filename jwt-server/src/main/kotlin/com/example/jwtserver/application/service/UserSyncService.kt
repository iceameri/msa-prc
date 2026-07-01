package com.example.jwtserver.application.service

import com.example.jwtserver.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class UserSyncService(private val userRepository: UserRepository) {

    // Kafka consumer는 version을 명시적으로 전달
    // 기회적 동기화(JWT에서 추출)는 version=0 — 새 유저면 INSERT, 기존 유저면 version < 0이 false라 무시됨
    fun sync(userId: Long, username: String, enabled: Boolean = true, status: String = "ACTIVE", version: Long = 0L) {
        userRepository.sync(userId, username, enabled, status, version)
    }

    // username 변경 전용 — enabled/status는 변경하지 않음
    fun syncUsername(userId: Long, newUsername: String, version: Long = 0L) {
        userRepository.syncUsername(userId, newUsername, version)
    }

    fun resolveId(username: String): Long? =
        userRepository.findByUsername(username)?.id
}
