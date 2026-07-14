package com.example.opaqueserver.infrastructure.ratelimit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import tools.jackson.core.type.TypeReference
import java.time.Duration
import java.time.Instant
import java.util.Base64

class OpenApiRateLimitFilter(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/openapi/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val apiKeyId = extractApiKeyId(request)
        if (apiKeyId == null) {
            chain.doFilter(request, response)
            return
        }

        val config = runCatching { redis.opsForValue().get("rl:config:$apiKeyId") }.getOrNull()
        val burst = config?.split(":")?.firstOrNull()?.toIntOrNull() ?: 100

        val window = Instant.now().epochSecond / 60
        val counterKey = "rl:count:$apiKeyId:$window"
        val count = runCatching {
            val c = redis.opsForValue().increment(counterKey)?.toInt() ?: 1
            if (c == 1) redis.expire(counterKey, Duration.ofSeconds(120))
            c
        }.getOrElse {
            log.warn("Rate limit Redis error for api_key_id={}, allowing request", apiKeyId)
            0
        }

        if (count > burst) {
            response.status = 429
            response.contentType = "application/json"
            response.writer.write("""{"error":"rate_limit_exceeded","limit":$burst}""")
            return
        }

        chain.doFilter(request, response)
    }

    private fun extractApiKeyId(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        val token = if (header.startsWith("Bearer ")) header.substring(7) else return null
        return runCatching {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val json = Base64.getUrlDecoder().decode(parts[1]).toString(Charsets.UTF_8)
            val claims = objectMapper.readValue(json, object : TypeReference<Map<String, Any?>>() {})
            claims["api_key_id"] as? String
        }.getOrNull()
    }
}
