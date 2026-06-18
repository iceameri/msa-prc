package com.example.authorizationserver.presentation

import com.example.authorizationserver.application.service.QrLoginService
import com.example.authorizationserver.domain.qr.QrLoginStatus
import com.example.authorizationserver.presentation.dto.QrConsumeResponse
import com.example.authorizationserver.presentation.dto.QrGenerateResponse
import com.example.authorizationserver.presentation.dto.QrStatusResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/qr-login")
class QrLoginController(private val qrLoginService: QrLoginService) {

    @GetMapping("/generate")
    fun generate(): ResponseEntity<QrGenerateResponse> {
        val result = qrLoginService.generateQrSession()
        return ResponseEntity.ok(QrGenerateResponse(result.token, result.qrCodeBase64))
    }

    @GetMapping("/status/{token}")
    fun status(@PathVariable token: String): ResponseEntity<QrStatusResponse> {
        val status = qrLoginService.getStatus(token)
        return if (status == QrLoginStatus.EXPIRED) ResponseEntity.notFound().build()
        else ResponseEntity.ok(QrStatusResponse(status.name))
    }

    // 모바일 앱에서 스캔 후 호출 (인증된 사용자만)
    @PostMapping("/confirm/{token}")
    fun confirm(
        @PathVariable token: String,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Void> {
        return if (qrLoginService.confirm(token, user.username)) ResponseEntity.ok().build()
        else ResponseEntity.badRequest().build()
    }

    // 브라우저 폴링이 CONFIRMED 감지 후 호출 — 세션 교환
    @PostMapping("/consume/{token}")
    fun consume(@PathVariable token: String): ResponseEntity<QrConsumeResponse> {
        val username = qrLoginService.consumeConfirmedSession(token)
            ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(QrConsumeResponse(username))
    }
}
