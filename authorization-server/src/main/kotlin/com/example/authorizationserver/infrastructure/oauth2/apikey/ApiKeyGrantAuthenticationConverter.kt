package com.example.authorizationserver.infrastructure.oauth2.apikey

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.authentication.AuthenticationConverter

class ApiKeyGrantAuthenticationConverter : AuthenticationConverter {

    override fun convert(request: HttpServletRequest): ApiKeyGrantAuthenticationToken? {
        val grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE) ?: return null
        if (grantType != ApiKeyGrantAuthenticationToken.GRANT_TYPE.value) return null

        val clientPrincipal = SecurityContextHolder.getContext().authentication
            ?: throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)

        val apiKey = request.getParameter("api_key")
            ?: throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST)

        return ApiKeyGrantAuthenticationToken(rawApiKey = apiKey, clientPrincipal = clientPrincipal)
    }
}
