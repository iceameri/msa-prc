package com.example.authorizationserver.infrastructure.config

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.domain.user.UserRepository
import com.example.authorizationserver.infrastructure.security.TenantAwareUserDetails
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

private val API_KEY_GRANT_TYPE = AuthorizationGrantType("urn:example:grant-type:api-key")

@Configuration
class OAuth2TokenCustomizerConfig(
    private val userRepository: UserRepository,
    private val userCachePort: UserCachePort
) {

    @Bean
    fun tokenCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            when (context.authorizationGrantType) {
                AuthorizationGrantType.CLIENT_CREDENTIALS -> {
                    context.claims.claim("client_id", context.registeredClient.clientId)
                    return@OAuth2TokenCustomizer
                }
                API_KEY_GRANT_TYPE -> return@OAuth2TokenCustomizer  // claims set directly by ApiKeyGrantAuthenticationProvider
            }

            val principal = context.getPrincipal<Authentication>()
            val username = principal?.name ?: return@OAuth2TokenCustomizer

            // TenantAwareUserDetails is available on first login (auth code flow)
            val userDetails = principal.principal as? TenantAwareUserDetails
            val userId = userDetails?.userId
            val tenantId = userDetails?.tenantId

            // Fallback for refresh token flow where principal may be reconstructed without TenantAwareUserDetails
            val resolvedUserId = userId ?: run {
                (userCachePort.getUser(username) ?: userRepository.findByUsername(username))?.id
            }

            if (context.tokenType.value == OidcParameterNames.ID_TOKEN) {
                context.claims.subject(resolvedUserId?.toString() ?: username)
                context.claims.claim("preferred_username", username)
            } else {
                context.claims.claim("user_id", resolvedUserId?.toString() ?: "")
                context.claims.claim("username", username)
                if (tenantId != null) {
                    context.claims.claim("tenant_id", tenantId.toString())
                }
            }
        }

    @Bean
    fun opaqueTokenCustomizer(): OAuth2TokenCustomizer<OAuth2TokenClaimsContext> =
        OAuth2TokenCustomizer { context ->
            if (context.authorizationGrantType == AuthorizationGrantType.CLIENT_CREDENTIALS) {
                // sub remains client_id (Spring default) — introspector detects by sub.toLongOrNull() == null
                return@OAuth2TokenCustomizer
            }
            val principal = context.getPrincipal<Authentication>()
            val username = principal?.name ?: return@OAuth2TokenCustomizer

            val userDetails = principal.principal as? TenantAwareUserDetails
            val userId = userDetails?.userId
                ?: (userCachePort.getUser(username) ?: userRepository.findByUsername(username))?.id

            context.claims.claim("sub", userId?.toString() ?: username)
            context.claims.claim("username", username)
            if (userDetails?.tenantId != null) {
                context.claims.claim("tenant_id", userDetails.tenantId.toString())
            }
        }
}
