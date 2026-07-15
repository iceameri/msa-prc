package com.example.opaqueserver.infrastructure.persistence

import com.example.opaqueserver.domain.notification.Notification
import com.example.opaqueserver.domain.notification.NotificationStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

@Repository
class NotificationJdbcRepository(private val jdbcTemplate: JdbcTemplate) {

    fun save(notification: Notification): Notification {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                """
                INSERT INTO opaque_db.public.notifications (recipient_id, recipient_username, type, content)
                VALUES (?, ?, ?, ?)
                """.trimMargin(),
                arrayOf("id")
            ).apply {
                setString(1, notification.recipientId)
                setString(2, notification.recipientUsername)
                setString(3, notification.type)
                setString(4, notification.content)
            }
        }, keyHolder)
        return notification.copy(id = keyHolder.key!!.toLong())
    }

    fun updateStatus(id: Long, status: NotificationStatus) {
        val sql = if (status == NotificationStatus.SENT)
            """
            UPDATE  opaque_db.public.notifications
            SET     status = ?, sent_at = NOW()
            WHERE   id = ?
            """.trimMargin()
        else
            """
            UPDATE  opaque_db.public.notifications
            SET     status = ?
            WHERE   id = ?
            """.trimMargin()
        jdbcTemplate.update(sql, status.name, id)
    }
}
