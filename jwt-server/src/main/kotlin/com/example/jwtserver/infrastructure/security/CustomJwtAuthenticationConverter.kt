package com.example.jwtserver.infrastructure.security

import com.example.jwtserver.application.port.out.AuthoritiesCachePort
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CustomJwtAuthenticationConverter(
    private val authoritiesCachePort: AuthoritiesCachePort
) : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val username = jwt.subject
        val authorities = authoritiesCachePort.getAuthorities(username)
            ?: emptySet()

        return JwtAuthenticationToken(
            jwt,
            authorities.map { SimpleGrantedAuthority(it) },
            username
        )
    }
}
