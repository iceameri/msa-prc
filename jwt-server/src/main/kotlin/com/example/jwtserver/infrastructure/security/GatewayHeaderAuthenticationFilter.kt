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
        if (userIdStr != null && username != null) {
            val userId = userIdStr.toLongOrNull()
            if (userId != null) {
                val authoritiesHeader = request.getHeader("X-User-Authorities") ?: ""
                val roles = authoritiesHeader.split(",").filter { it.isNotBlank() }.map { it.trim() }
                val authorities = roles.map { SimpleGrantedAuthority(it) }
                val auth = UsernamePasswordAuthenticationToken(AuthenticatedUser(userId, username, roles), null, authorities)
                val context = SecurityContextHolder.createEmptyContext()
                context.authentication = auth
                SecurityContextHolder.setContext(context)
            }
        }
        filterChain.doFilter(request, response)
    }
}
