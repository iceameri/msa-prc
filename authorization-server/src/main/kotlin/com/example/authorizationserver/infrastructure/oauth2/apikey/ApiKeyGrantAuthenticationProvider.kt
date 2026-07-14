package com.example.authorizationserver.infrastructure.oauth2.apikey

import com.example.authorizationserver.application.service.ApiKeyService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import java.time.Instant

class ApiKeyGrantAuthenticationProvider(
    private val apiKeyService: ApiKeyService,
    private val authorizationService: OAuth2AuthorizationService,
    private val jwtEncoder: JwtEncoder,
    private val authorizationServerSettings: AuthorizationServerSettings
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication as ApiKeyGrantAuthenticationToken
        val clientPrincipal = token.principal as? OAuth2ClientAuthenticationToken
            ?: throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)
        val registeredClient = clientPrincipal.registeredClient
            ?: throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)

        if (!registeredClient.authorizationGrantTypes.contains(ApiKeyGrantAuthenticationToken.GRANT_TYPE)) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT)
        }

        val apiKey = apiKeyService.validateAndGet(token.rawApiKey)
            ?: throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST)

        val now = Instant.now()
        val expiry = now.plus(registeredClient.tokenSettings.accessTokenTimeToLive)

        val claims = JwtClaimsSet.builder()
            .issuer(authorizationServerSettings.issuer)
            .issuedAt(now)
            .expiresAt(expiry)
            .subject(registeredClient.clientId)
            .claim("client_id", registeredClient.clientId)
            .claim("tenant_id", apiKey.tenantId.toString())
            .claim("api_key_id", apiKey.id!!.toString())
            .claim("scope", registeredClient.scopes.joinToString(" "))
            .build()

        val jwt = jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
            )
        )

        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            jwt.tokenValue,
            now,
            expiry,
            registeredClient.scopes
        )

        val authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(registeredClient.clientId)
            .authorizationGrantType(ApiKeyGrantAuthenticationToken.GRANT_TYPE)
            .accessToken(accessToken)
            .attribute("tenant_id", apiKey.tenantId)
            .attribute("api_key_id", apiKey.id)
            .build()

        authorizationService.save(authorization)

        return OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken)
    }

    override fun supports(authentication: Class<*>): Boolean =
        ApiKeyGrantAuthenticationToken::class.java.isAssignableFrom(authentication)
}
