package com.example.authorizationserver.application.service

import com.example.authorizationserver.application.port.out.PasswordResetCachePort
import com.example.authorizationserver.domain.user.UserRepository
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PasswordResetService(
    private val userRepository: UserRepository,
    private val passwordResetCachePort: PasswordResetCachePort,
    private val mailSender: JavaMailSender,
    private val passwordEncoder: PasswordEncoder
) {
    fun requestReset(email: String) {
        val user = userRepository.findByEmail(email) ?: return  // 존재 여부 노출 방지용 silent return

        val token = UUID.randomUUID().toString()
        passwordResetCachePort.saveToken(token, user.username)

        val message = SimpleMailMessage().apply {
            setTo(email)
            subject = "[msa-prc] 비밀번호 재설정"
            text = "아래 링크를 클릭하여 비밀번호를 재설정하세요 (30분 유효):\n" +
                    "http://localhost:1010/password/reset?token=$token"
        }
        mailSender.send(message)
    }

    fun resetPassword(token: String, newPassword: String): Boolean {
        val username = passwordResetCachePort.getUsernameByToken(token) ?: return false
        val user = userRepository.findByUsername(username) ?: return false

        passwordEncoder.encode(newPassword)?.let { userRepository.save(user.copy(password = it)) }
        passwordResetCachePort.deleteToken(token)
        return true
    }
}
