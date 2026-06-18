package com.example.opaqueserver.domain.payment

interface PaymentSagaRepository {
    fun save(saga: PaymentSaga): PaymentSaga
    fun findByPaymentId(paymentId: Long): List<PaymentSaga>
}
