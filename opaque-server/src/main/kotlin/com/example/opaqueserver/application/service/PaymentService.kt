package com.example.opaqueserver.application.service

import com.example.opaqueserver.application.port.out.EventPublishPort
import com.example.opaqueserver.domain.payment.*
import com.example.opaqueserver.infrastructure.persistence.PaymentJdbcRepository
import com.example.opaqueserver.infrastructure.persistence.PaymentSagaJdbcRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class PaymentService(
    private val paymentRepository: PaymentJdbcRepository,
    private val paymentSagaRepository: PaymentSagaJdbcRepository,
    private val auditService: AuditService,
    private val eventPublishPort: EventPublishPort
) {

    @Transactional
    fun initiate(userId: Long, username: String, orderId: String, amount: BigDecimal): Payment {
        check(paymentRepository.findByOrderId(orderId) == null) { "Order already exists: $orderId" }
        val payment = paymentRepository.save(Payment(userId = userId, orderId = orderId, amount = amount))
        paymentSagaRepository.save(PaymentSaga(paymentId = payment.id!!, step = SagaStep.INITIATED, status = SagaStatus.IN_PROGRESS))
        auditService.log(userId.toString(), username, "PAYMENT_INITIATED", "PAYMENT", payment.id.toString())
        eventPublishPort.publish("payment.saga", orderId,
            """{"event":"PAYMENT_INITIATED","paymentId":${payment.id},"orderId":"$orderId","amount":$amount}""")
        return payment
    }

    @Transactional
    fun complete(paymentId: Long) {
        val payment = paymentRepository.findById(paymentId) ?: throw NoSuchElementException("Payment not found: $paymentId")
        paymentRepository.updateStatus(paymentId, PaymentStatus.COMPLETED)
        paymentSagaRepository.save(PaymentSaga(paymentId = paymentId, step = SagaStep.COMPLETED, status = SagaStatus.SUCCESS))
        eventPublishPort.publish("payment.saga", payment.orderId,
            """{"event":"PAYMENT_COMPLETED","paymentId":$paymentId,"orderId":"${payment.orderId}"}""")
    }

    @Transactional
    fun fail(paymentId: Long, reason: String) {
        paymentRepository.findById(paymentId) ?: throw NoSuchElementException("Payment not found: $paymentId")
        paymentRepository.updateStatus(paymentId, PaymentStatus.FAILED)
        paymentSagaRepository.save(PaymentSaga(paymentId = paymentId, step = SagaStep.COMPENSATION, status = SagaStatus.IN_PROGRESS, detail = reason))
        eventPublishPort.publish("payment.saga", paymentId.toString(),
            """{"event":"PAYMENT_FAILED","paymentId":$paymentId,"reason":"$reason"}""")
    }

    fun getById(paymentId: Long): Payment =
        paymentRepository.findById(paymentId) ?: throw NoSuchElementException("Payment not found: $paymentId")

    fun getByIdAndUserId(paymentId: Long, userId: Long): Payment =
        paymentRepository.findByIdAndUserId(paymentId, userId)
            ?: throw NoSuchElementException("Payment not found: $paymentId")

    fun getByUserId(userId: Long): List<Payment> =
        paymentRepository.findByUserId(userId)

    fun getSagaHistory(paymentId: Long): List<PaymentSaga> =
        paymentSagaRepository.findByPaymentId(paymentId)
}
