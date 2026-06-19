package com.example.jwtserver.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class GatewayHeaderAuthenticationFilter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val userIdStr = request.getHeader("X-User-Id")
        val username = request.getHeader("X-User-Name")
        val clientId = request.getHeader("X-Client-Id")
        when {
            userIdStr != null && username != null -> {
                val userId = userIdStr.toLongOrNull()
                if (userId != null) {
                    val authoritiesHeader = request.getHeader("X-User-Authorities") ?: ""
                    val roles = authoritiesHeader.split(",").filter { it.isNotBlank() }.map { it.trim() }
                    val auth = UsernamePasswordAuthenticationToken(
                        AuthenticatedUser(userId, username, roles), null, roles.map { SimpleGrantedAuthority(it) }
                    )
                    val context = SecurityContextHolder.createEmptyContext()
                    context.authentication = auth
                    SecurityContextHolder.setContext(context)
                }
            }
            clientId != null -> {
                val auth = UsernamePasswordAuthenticationToken(
                    AuthenticatedClient(clientId), null, listOf(SimpleGrantedAuthority("ROLE_SYSTEM"))
                )
                val context = SecurityContextHolder.createEmptyContext()
                context.authentication = auth
                SecurityContextHolder.setContext(context)
            }
        }
        filterChain.doFilter(request, response)
    }
}
