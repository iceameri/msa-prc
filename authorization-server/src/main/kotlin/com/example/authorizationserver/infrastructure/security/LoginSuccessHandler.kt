package com.example.authorizationserver.infrastructure.security

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.application.service.LoginAttemptService
import com.example.authorizationserver.infrastructure.persistence.UserJdbcRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class LoginSuccessHandler(
    private val loginAttemptService: LoginAttemptService,
    private val userCachePort: UserCachePort,
    private val userRepository: UserJdbcRepository
) : SavedRequestAwareAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val username = authentication.name
        val user = userRepository.findByUsername(username)

        if (user?.mfaEnabled == true) {
            SecurityContextHolder.clearContext()
            request.getSession(true).setAttribute("MFA_PENDING_USER", username)
            response.sendRedirect("/mfa/verify")
            return
        }

        loginAttemptService.onLoginSuccess(username)
        user?.id?.let { userId ->
            userCachePort.saveAuthorities(userId, authentication.authorities.mapNotNull { it.authority }.toSet())
        }
        super.onAuthenticationSuccess(request, response, authentication)
    }
}
