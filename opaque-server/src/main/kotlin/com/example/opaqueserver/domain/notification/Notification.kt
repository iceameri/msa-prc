package com.example.opaqueserver.domain.notification

import java.time.Instant

data class Notification(
    val id: Long? = null,
    val recipientId: String,
    val recipientUsername: String,
    val type: String,
    val content: String,
    val status: NotificationStatus = NotificationStatus.PENDING,
    val sentAt: Instant? = null,
    val createdAt: Instant = Instant.now()
)

enum class NotificationStatus { PENDING, SENT, FAILED }
