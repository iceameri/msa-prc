package com.example.opaqueserver.infrastructure.kafka

import com.example.opaqueserver.application.service.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentSagaConsumer(
    private val paymentService: PaymentService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(PaymentSagaConsumer::class.java)

    @KafkaListener(topics = ["payment.saga"], groupId = "opaque-server-group")
    fun consume(message: String) {
        try {
            val payload = objectMapper.readValue(message, Map::class.java)
            val event = payload["event"]?.toString() ?: return
            val paymentId = (payload["paymentId"] as Number).toLong()
            when (event) {
                "PAYMENT_INITIATED" -> paymentService.complete(paymentId)  // 실제 환경에서는 외부 결제 게이트웨이 호출
                "PAYMENT_FAILED"    -> log.warn("Payment failed: paymentId={}", paymentId)
            }
        } catch (ex: Exception) {
            log.error("Failed to process payment saga event: {}", ex.message)
        }
    }
}
