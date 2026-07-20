package com.example.authorizationserver.infrastructure.security

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.infrastructure.oauth2.TokenRevocationService
import com.example.authorizationserver.infrastructure.security.TenantAwareUserDetails
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
        val userId = (authentication.principal as? TenantAwareUserDetails)?.userId
        if (userId != null) userCachePort.deleteAuthorities(userId)
        userCachePort.deleteUser(username)
        tokenRevocationService.revokeAllForPrincipal(username)
    }
}
