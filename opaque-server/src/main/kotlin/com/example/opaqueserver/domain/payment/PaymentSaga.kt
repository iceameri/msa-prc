package com.example.opaqueserver.domain.payment

import java.time.Instant

data class PaymentSaga(
    val id: Long? = null,
    val paymentId: Long,
    val step: SagaStep,
    val status: SagaStatus,
    val detail: String? = null,
    val createdAt: Instant = Instant.now()
)

enum class SagaStep { INITIATED, PROCESSING, COMPLETED, COMPENSATION }
enum class SagaStatus { SUCCESS, FAILED, IN_PROGRESS }
