package com.example.jwtserver.infrastructure.security

import com.example.jwtserver.application.port.out.AuthoritiesCachePort
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.util.Base64

@Component
class JwtClaimsFilter(
    private val authoritiesCachePort: AuthoritiesCachePort,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        extractToken(request)?.let { token ->
            runCatching { buildAuthentication(parseClaims(token)) }
                .onSuccess { SecurityContextHolder.getContext().authentication = it }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.substring(7) else null
    }

    private fun parseClaims(token: String): Map<String, Any?> {
        val payload = token.split(".").getOrNull(1) ?: error("Invalid JWT structure")
        val json = Base64.getUrlDecoder().decode(payload).toString(Charsets.UTF_8)
        return objectMapper.readValue(json, object : TypeReference<Map<String, Any?>>() {})
    }

    private fun buildAuthentication(claims: Map<String, Any?>): UsernamePasswordAuthenticationToken {
        val userId = claims["user_id"] as? String

        if (userId != null) {
            val username = claims["sub"] as String
            val roles = authoritiesCachePort.getAuthorities(userId.toLong())?.toList() ?: emptyList()
            return UsernamePasswordAuthenticationToken(
                AuthenticatedUser(userId.toLong(), username, roles),
                null,
                roles.map { SimpleGrantedAuthority(it) }
            )
        }

        val clientId = claims["client_id"] as? String ?: claims["sub"] as String
        return UsernamePasswordAuthenticationToken(
            AuthenticatedClient(clientId),
            null,
            listOf(SimpleGrantedAuthority("ROLE_SYSTEM"))
        )
    }
}
