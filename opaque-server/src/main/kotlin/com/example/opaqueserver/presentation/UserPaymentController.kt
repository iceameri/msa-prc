package com.example.opaqueserver.presentation

import com.example.opaqueserver.application.service.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payments")
@PreAuthorize("hasRole('USER')")
class UserPaymentController(private val paymentService: PaymentService) {

    private fun OAuth2AuthenticatedPrincipal.userId(): Long =
        attributes["sub"]?.toString()?.toLongOrNull()
            ?: throw IllegalStateException("Missing sub claim")

    private fun OAuth2AuthenticatedPrincipal.username(): String =
        attributes["username"]?.toString() ?: name

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun initiate(
        @AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal,
        @RequestBody req: InitiatePaymentRequest
    ): PaymentResponse {
        return PaymentResponse.from(
            paymentService.initiate(principal.userId(), principal.username(), req.orderId, req.amount)
        )
    }

    @GetMapping("/{id}")
    fun getById(
        @AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal,
        @PathVariable id: Long
    ): PaymentResponse =
        PaymentResponse.from(paymentService.getByIdAndUserId(id, principal.userId()))

    @GetMapping
    fun getMyPayments(
        @AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal
    ): List<PaymentResponse> =
        paymentService.getByUserId(principal.userId()).map { PaymentResponse.from(it) }
}
