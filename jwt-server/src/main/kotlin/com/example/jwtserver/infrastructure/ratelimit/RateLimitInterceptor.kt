package com.example.jwtserver.infrastructure.ratelimit

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

// /api/**: 20 req/s, burst 40 — gateway-server의 jwt-server 레이트 리미팅과 동일
@Component
class RateLimitInterceptor(private val rateLimiter: RedisRateLimiter) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val name = SecurityContextHolder.getContext().authentication?.name ?: request.remoteAddr
        val key = "rl:api:$name"

        if (rateLimiter.tryConsume(key, capacity = 40, refillPerSecond = 20)) return true

        response.status = 429
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""{"status":429,"error":"Too Many Requests","path":"${request.requestURI}"}""")
        return false
    }
}
