package com.example.authorizationserver.infrastructure.config

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.domain.user.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

@Configuration
class OAuth2TokenCustomizerConfig(
    private val userRepository: UserRepository,
    private val userCachePort: UserCachePort
) {

    @Bean
    fun tokenCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            if (context.authorizationGrantType == AuthorizationGrantType.CLIENT_CREDENTIALS) {
                context.claims.claim("client_id", context.registeredClient.clientId)
                return@OAuth2TokenCustomizer
            }
            val username = context.getPrincipal<Authentication>()?.name ?: return@OAuth2TokenCustomizer
            val user = userCachePort.getUser(username)
                ?: userRepository.findByUsername(username)
                ?: return@OAuth2TokenCustomizer
            if (context.tokenType.value == OidcParameterNames.ID_TOKEN) {
                // id_token: OIDC 표준 클레임 — sub를 안정적 식별자(user_id)로 덮어씀
                context.claims.subject(user.id?.toString() ?: username)
                context.claims.claim("preferred_username", user.username)
            } else {
                // access_token: jwt-server JwtClaimsFilter가 읽는 앱 전용 클레임
                context.claims.claim("user_id", user.id?.toString() ?: "")
                context.claims.claim("username", user.username)
            }
        }

    @Bean
    fun opaqueTokenCustomizer(): OAuth2TokenCustomizer<OAuth2TokenClaimsContext> =
        OAuth2TokenCustomizer { context ->
            if (context.authorizationGrantType == AuthorizationGrantType.CLIENT_CREDENTIALS) {
                // sub remains client_id (Spring default) — introspector detects by sub.toLongOrNull() == null
                return@OAuth2TokenCustomizer
            }
            val username = context.getPrincipal<Authentication>()?.name ?: return@OAuth2TokenCustomizer
            val user = userCachePort.getUser(username)
                ?: userRepository.findByUsername(username)
                ?: return@OAuth2TokenCustomizer
            context.claims.claim("sub", user.id?.toString() ?: username)
            context.claims.claim("username", user.username)
        }
}
