package com.example.opaqueserver.domain.payment

import java.math.BigDecimal
import java.time.Instant

data class Payment(
    val id: Long? = null,
    val userId: Long,
    val orderId: String,
    val amount: BigDecimal,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class PaymentStatus { PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED }
