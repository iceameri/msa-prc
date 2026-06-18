package com.example.authorizationserver.presentation

import com.example.authorizationserver.application.service.PasswordResetService
import com.example.authorizationserver.presentation.dto.ResetConfirmBody
import com.example.authorizationserver.presentation.dto.ResetRequestBody
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/password")
class PasswordResetController(private val passwordResetService: PasswordResetService) {

    @PostMapping("/reset-request")
    fun requestReset(@RequestBody req: ResetRequestBody): ResponseEntity<Void> {
        passwordResetService.requestReset(req.email)
        return ResponseEntity.ok().build()  // 이메일 존재 여부 무관하게 200 반환
    }

    @PostMapping("/reset")
    fun reset(@RequestBody req: ResetConfirmBody): ResponseEntity<Void> {
        return if (passwordResetService.resetPassword(req.token, req.newPassword))
            ResponseEntity.ok().build()
        else
            ResponseEntity.badRequest().build()
    }
}
