package com.example.jwtserver.presentation

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/openapi")
class OpenApiController {

    @GetMapping("/ping")
    fun ping(@AuthenticationPrincipal principal: Any?): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.ok(mapOf("status" to "ok", "principal" to principal?.toString()))
}
