package com.example.opaqueserver.infrastructure.kafka

import com.example.opaqueserver.application.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class NotificationEventConsumer(
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(NotificationEventConsumer::class.java)

    @KafkaListener(topics = ["notifications"], groupId = "opaque-server-group")
    fun consume(message: String) {
        try {
            val payload = objectMapper.readValue(message, Map::class.java)
            val type = payload["type"]?.toString() ?: return
            val target = (payload["target"] ?: payload["targetUsername"])?.toString() ?: return
            val targetId = payload["targetId"]?.toString() ?: ""
            val actor = (payload["actor"] ?: payload["actorUsername"])?.toString() ?: "system"
            val content = buildContent(type, actor, payload)
            notificationService.send(targetId, target, type, content)
        } catch (ex: Exception) {
            log.error("Failed to process notification event: {}", ex.message)
        }
    }

    private fun buildContent(type: String, actor: String, payload: Map<*, *>): String = when (type) {
        "POST_LIKED"     -> "$actor 님이 회원님의 게시글을 좋아합니다. (postId=${payload["postId"]})"
        "POST_COMMENTED" -> "$actor 님이 회원님의 게시글에 댓글을 달았습니다. (postId=${payload["postId"]})"
        "USER_FOLLOWED"  -> "$actor 님이 회원님을 팔로우하기 시작했습니다."
        else             -> "새 알림이 있습니다 ($actor)"
    }
}
