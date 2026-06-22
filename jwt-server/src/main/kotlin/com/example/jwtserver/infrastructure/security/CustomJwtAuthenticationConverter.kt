package com.example.jwtserver.infrastructure.security

import com.example.jwtserver.application.port.out.AuthoritiesCachePort
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class CustomJwtAuthenticationConverter(
    private val authoritiesCachePort: AuthoritiesCachePort
) : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val userId = jwt.getClaim<String>("user_id")

        if (userId != null) {
            val username = jwt.subject
            val roles = authoritiesCachePort.getAuthorities(username)?.toList() ?: emptyList()
            return UsernamePasswordAuthenticationToken(
                AuthenticatedUser(userId.toLong(), username, roles),
                null,
                roles.map { SimpleGrantedAuthority(it) }
            )
        }

        val clientId = jwt.getClaim<String>("client_id") ?: jwt.subject
        return UsernamePasswordAuthenticationToken(
            AuthenticatedClient(clientId),
            null,
            listOf(SimpleGrantedAuthority("ROLE_SYSTEM"))
        )
    }
}
