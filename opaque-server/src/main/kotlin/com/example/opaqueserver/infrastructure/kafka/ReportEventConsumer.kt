package com.example.opaqueserver.infrastructure.kafka

import com.example.opaqueserver.application.service.ReportProcessingService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReportEventConsumer(
    private val reportProcessingService: ReportProcessingService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(ReportEventConsumer::class.java)

    @KafkaListener(topics = ["reports"], groupId = "opaque-server-group")
    fun consume(message: String) {
        try {
            val payload = objectMapper.readValue(message, Map::class.java)
            reportProcessingService.receiveFromKafka(
                externalId = (payload["reportId"] as Number).toLong(),
                reporterUsername = payload["reporterUsername"]?.toString() ?: "unknown",
                targetType = payload["targetType"]?.toString() ?: "",
                targetId = (payload["targetId"] as Number).toLong(),
                reason = payload["reason"]?.toString() ?: ""
            )
        } catch (ex: Exception) {
            log.error("Failed to process report event: {}", ex.message)
        }
    }
}
