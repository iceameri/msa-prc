package com.example.opaqueserver.infrastructure.ratelimit

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

// /admin/**: 10 req/s, burst 20
// /payments/**: 20 req/s, burst 40
// — gateway-server의 opaque-server 레이트 리미팅과 동일
@Component
class RateLimitInterceptor(private val rateLimiter: RedisRateLimiter) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val name = SecurityContextHolder.getContext().authentication?.name ?: request.remoteAddr
        val (key, capacity, refillPerSecond) = when {
            request.requestURI.startsWith("/admin") -> Triple("rl:admin:$name", 20L, 10L)
            request.requestURI.startsWith("/payments") -> Triple("rl:payments:$name", 40L, 20L)
            else -> return true
        }

        if (rateLimiter.tryConsume(key, capacity, refillPerSecond)) return true

        response.status = 429
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""{"status":429,"error":"Too Many Requests","path":"${request.requestURI}"}""")
        return false
    }
}
