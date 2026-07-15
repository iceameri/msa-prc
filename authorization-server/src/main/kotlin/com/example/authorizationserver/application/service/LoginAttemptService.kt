package com.example.authorizationserver.application.service

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.infrastructure.persistence.UserJdbcRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class LoginAttemptService(
    private val userCachePort: UserCachePort,
    private val userRepository: UserJdbcRepository
) {
    companion object {
        const val MAX_ATTEMPTS = 5
        private val LOCK_DURATION: Duration = Duration.ofMinutes(30)
    }

    fun onLoginSuccess(username: String) {
        userCachePort.resetLoginAttempts(username)
        userRepository.resetLoginAttempts(username)
    }

    fun onLoginFailure(username: String) {
        val attempts = userCachePort.incrementLoginAttempts(username)
        if (attempts >= MAX_ATTEMPTS) {
            userRepository.lockUser(username, Instant.now().plus(LOCK_DURATION))
        }
    }
}
