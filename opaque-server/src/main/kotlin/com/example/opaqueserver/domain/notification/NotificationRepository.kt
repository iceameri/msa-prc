package com.example.opaqueserver.domain.notification

interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun updateStatus(id: Long, status: NotificationStatus)
}
