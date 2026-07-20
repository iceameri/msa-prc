package com.example.opaqueserver.infrastructure.security

import com.example.opaqueserver.application.port.out.EventPublishPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.time.Instant

@Component
class CustomOpaqueTokenIntrospector(
    @Value("\${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}") private val introspectionUri: String,
    @Value("\${spring.security.oauth2.resourceserver.opaquetoken.client-id}") private val clientId: String,
    @Value("\${spring.security.oauth2.resourceserver.opaquetoken.client-secret}") private val clientSecret: String,
    private val eventPublishPort: EventPublishPort,
    private val redisTemplate: RedisTemplate<String, Any>
) : OpaqueTokenIntrospector {

    private val restClient: RestClient = RestClient.builder()
        .defaultHeaders { it.setBasicAuth(clientId, clientSecret) }
        .build()

    override fun introspect(token: String): OAuth2AuthenticatedPrincipal {
        val claims = restClient.post()
            .uri(introspectionUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(LinkedMultiValueMap<String, String>().apply { add("token", token) })
            .retrieve()
            .body(object : ParameterizedTypeReference<Map<String, Any>>() {})
            ?: throw OAuth2IntrospectionException("Empty introspection response")

        if (claims["active"] != true) {
            throw OAuth2IntrospectionException("Token is not active")
        }

        // Spring Security 7 expects exp/nbf/iat as Instant; JSON deserialization produces Integer/Long
        val normalizedClaims = claims.toMutableMap<String, Any>()
        for (key in listOf("exp", "nbf", "iat")) {
            val v = normalizedClaims[key]
            if (v is Number) normalizedClaims[key] = Instant.ofEpochSecond(v.toLong())
        }

        val sub = normalizedClaims["sub"]?.toString()
            ?: throw OAuth2IntrospectionException("Missing sub in introspection response")

        // client_credentials tokens have sub = client_id (non-numeric); user tokens have sub = userId (Long string)
        val userId = sub.toLongOrNull()
        if (userId == null) {
            return DefaultOAuth2AuthenticatedPrincipal(sub, normalizedClaims, listOf(SimpleGrantedAuthority("ROLE_SYSTEM")))
        }

        val username = normalizedClaims["username"]?.toString() ?: sub

        eventPublishPort.publish("user-active", sub, """{"userId":$userId}""")

        val authorities = (redisTemplate.opsForValue().get("jwt:authorities:$userId") as? Collection<*>)
            ?.filterIsInstance<String>()
            ?.map { SimpleGrantedAuthority(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: throw OAuth2IntrospectionException("Could not load authorities for user: $username")

        return DefaultOAuth2AuthenticatedPrincipal(username, normalizedClaims, authorities)
    }
}
