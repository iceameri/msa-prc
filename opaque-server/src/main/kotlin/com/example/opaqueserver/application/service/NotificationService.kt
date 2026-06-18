package com.example.opaqueserver.application.service

import com.example.opaqueserver.application.port.out.EmailPort
import com.example.opaqueserver.domain.notification.Notification
import com.example.opaqueserver.domain.notification.NotificationRepository
import com.example.opaqueserver.domain.notification.NotificationStatus
import com.example.opaqueserver.domain.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val emailPort: EmailPort
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    @Transactional
    fun send(recipientId: String, recipientUsername: String, type: String, content: String) {
        val notification = notificationRepository.save(
            Notification(recipientId = recipientId, recipientUsername = recipientUsername, type = type, content = content)
        )
        val email = userRepository.findById(recipientId)?.email
        if (email == null) {
            log.warn("No email for userId={}, notification queued", recipientId)
            return
        }
        try {
            emailPort.send(email, buildSubject(type), content)
            notificationRepository.updateStatus(notification.id!!, NotificationStatus.SENT)
        } catch (ex: Exception) {
            log.error("Failed to send email to userId={}: {}", recipientId, ex.message)
            notificationRepository.updateStatus(notification.id!!, NotificationStatus.FAILED)
        }
    }

    private fun buildSubject(type: String) = when (type) {
        "POST_LIKED"     -> "누군가 회원님의 게시글을 좋아합니다"
        "POST_COMMENTED" -> "누군가 회원님의 게시글에 댓글을 달았습니다"
        "USER_FOLLOWED"  -> "새로운 팔로워가 생겼습니다"
        "REPORT_CREATED" -> "[관리자] 신고가 접수되었습니다"
        else             -> "새 알림이 있습니다"
    }
}
