package com.example.jwtserver.application.service

import com.example.jwtserver.domain.user.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserSyncService(private val userRepository: UserRepository) {

    // Kafka consumer는 이벤트 발행 시각을 명시적으로 전달
    // 요청 중 기회적 동기화(JWT에서 추출)는 현재 시각을 기본값으로 사용
    fun sync(userId: Long, username: String, updatedAt: Instant = Instant.now()) {
        userRepository.sync(userId, username, updatedAt)
    }

    fun resolveId(username: String): Long? =
        userRepository.findByUsername(username)?.id
}
