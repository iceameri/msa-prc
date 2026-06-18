package com.example.opaqueserver.domain.payment

interface PaymentRepository {
    fun findById(id: Long): Payment?
    fun findByOrderId(orderId: String): Payment?
    fun findByIdAndUserId(id: Long, userId: Long): Payment?
    fun findByUserId(userId: Long): List<Payment>
    fun save(payment: Payment): Payment
    fun updateStatus(id: Long, status: PaymentStatus)
    fun sumCompletedAmount(): java.math.BigDecimal
}
