package com.example.opaqueserver.application.service

import com.example.opaqueserver.domain.outbox.OutboxEvent
import com.example.opaqueserver.domain.outbox.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserManagementService(
    private val auditService: AuditService,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun suspend(actorId: String, actorUsername: String, targetUserId: Long) {
        auditService.log(actorId, actorUsername, "SUSPEND_USER", "USER", targetUserId.toString())
        outboxRepository.save(buildOutboxEvent("SUSPEND", targetUserId, actorUsername))
    }

    @Transactional
    fun ban(actorId: String, actorUsername: String, targetUserId: Long) {
        auditService.log(actorId, actorUsername, "BAN_USER", "USER", targetUserId.toString())
        outboxRepository.save(buildOutboxEvent("BAN", targetUserId, actorUsername))
    }

    @Transactional
    fun restore(actorId: String, actorUsername: String, targetUserId: Long) {
        auditService.log(actorId, actorUsername, "RESTORE_USER", "USER", targetUserId.toString())
        outboxRepository.save(buildOutboxEvent("RESTORE", targetUserId, actorUsername))
    }

    @Transactional
    fun delete(actorId: String, actorUsername: String, targetUserId: Long) {
        auditService.log(actorId, actorUsername, "DELETE_USER", "USER", targetUserId.toString())
        outboxRepository.save(buildOutboxEvent("DELETE", targetUserId, actorUsername))
    }

    private fun buildOutboxEvent(action: String, userId: Long, actor: String) = OutboxEvent(
        aggregateId = userId.toString(),
        aggregateType = "USER",
        eventType = "${action}_USER",
        payload = objectMapper.writeValueAsString(
            mapOf("action" to action, "userId" to userId, "actor" to actor)
        )
    )
}
