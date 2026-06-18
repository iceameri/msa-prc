package com.example.jwtserver.application.service

import com.example.jwtserver.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class UserSyncService(private val userRepository: UserRepository) {

    fun sync(userId: Long, username: String) {
        userRepository.sync(userId, username)
    }

    fun resolveId(username: String): Long? =
        userRepository.findByUsername(username)?.id
}
