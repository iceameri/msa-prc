package com.example.authorizationserver.infrastructure.security

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.infrastructure.oauth2.TokenRevocationService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Component

@Component
class RedisLogoutHandler(
    private val userCachePort: UserCachePort,
    private val tokenRevocationService: TokenRevocationService
) : LogoutHandler {

    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        val username = authentication?.name ?: return
        userCachePort.deleteAuthorities(username)
        userCachePort.deleteUser(username)
        tokenRevocationService.revokeAllForPrincipal(username)
    }
}
