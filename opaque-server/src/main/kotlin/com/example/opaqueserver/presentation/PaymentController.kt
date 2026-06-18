package com.example.opaqueserver.presentation

import com.example.opaqueserver.application.service.PaymentService
import com.example.opaqueserver.domain.payment.Payment
import com.example.opaqueserver.domain.payment.PaymentSaga
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

data class InitiatePaymentRequest(val orderId: String, val amount: BigDecimal)
data class PaymentResponse(val id: Long, val orderId: String, val amount: BigDecimal, val status: String) {
    companion object { fun from(p: Payment) = PaymentResponse(p.id!!, p.orderId, p.amount, p.status.name) }
}

@RestController
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun initiate(
        @AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal,
        @RequestBody req: InitiatePaymentRequest
    ): PaymentResponse {
        val userId = principal.attributes["sub"]?.toString()?.toLongOrNull()
            ?: throw IllegalStateException("Missing sub claim")
        val username = principal.attributes["username"]?.toString() ?: principal.name
        return PaymentResponse.from(paymentService.initiate(userId, username, req.orderId, req.amount))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): PaymentResponse =
        PaymentResponse.from(paymentService.getById(id))

    @GetMapping("/{id}/saga")
    fun getSagaHistory(@PathVariable id: Long): List<PaymentSaga> =
        paymentService.getSagaHistory(id)
}
