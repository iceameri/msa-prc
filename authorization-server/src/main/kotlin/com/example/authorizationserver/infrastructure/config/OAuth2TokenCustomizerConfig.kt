package com.example.authorizationserver.infrastructure.config

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.domain.user.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

@Configuration
class OAuth2TokenCustomizerConfig(
    private val userRepository: UserRepository,
    private val userCachePort: UserCachePort
) {

    @Bean
    fun tokenCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            val username = context.getPrincipal<Authentication>()?.name ?: return@OAuth2TokenCustomizer
            val user = userCachePort.getUser(username)
                ?: userRepository.findByUsername(username)
                ?: return@OAuth2TokenCustomizer
            context.claims.claim("user_id", user.id?.toString() ?: "")
            context.claims.claim("username", user.username)
        }
}
