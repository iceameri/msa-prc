package com.example.authorizationserver.infrastructure.oauth2.apikey

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken

class ApiKeyGrantAuthenticationToken(
    val rawApiKey: String,
    clientPrincipal: Authentication,
    val resolvedTenantId: Long? = null,
    val resolvedApiKeyId: Long? = null,
    additionalParameters: Map<String, Any> = emptyMap()
) : OAuth2AuthorizationGrantAuthenticationToken(GRANT_TYPE, clientPrincipal, additionalParameters) {

    companion object {
        val GRANT_TYPE = AuthorizationGrantType("urn:example:grant-type:api-key")
    }
}
