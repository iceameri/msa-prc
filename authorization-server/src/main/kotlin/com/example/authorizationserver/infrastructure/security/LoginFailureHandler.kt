package com.example.authorizationserver.infrastructure.security

import com.example.authorizationserver.application.service.LoginAttemptService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class LoginFailureHandler(
    private val loginAttemptService: LoginAttemptService
) : SimpleUrlAuthenticationFailureHandler("/login?error") {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        request.getParameter("username")?.let {
            loginAttemptService.onLoginFailure(it)
        }
        super.onAuthenticationFailure(request, response, exception)
    }
}
