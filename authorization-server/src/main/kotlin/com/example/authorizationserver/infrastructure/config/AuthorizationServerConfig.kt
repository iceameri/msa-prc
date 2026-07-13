package com.example.authorizationserver.infrastructure.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

@Configuration
class AuthorizationServerConfig(
    @Value("\${spring.security.oauth2.authorizationserver.issuer}")
    private val issuer: String,
    // K8s: Secret 볼륨 마운트 경로 (비어있으면 로컬 개발용 인메모리 키 생성)
    @Value("\${rsa.key-path:}")
    private val rsaKeyPath: String
) {

    @Bean
    fun registeredClientRepository(jdbcTemplate: JdbcTemplate): RegisteredClientRepository =
        JdbcRegisteredClientRepository(jdbcTemplate)

    @Bean
    fun authorizationService(
        jdbcTemplate: JdbcTemplate,
        registeredClientRepository: RegisteredClientRepository,
        stringRedisTemplate: StringRedisTemplate,
        circuitBreakerRegistry: CircuitBreakerRegistry
    ): OAuth2AuthorizationService =
        com.example.authorizationserver.infrastructure.oauth2.SessionLimitingAuthorizationService(
            com.example.authorizationserver.infrastructure.oauth2.RedisBackedOAuth2AuthorizationService(
                JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository),
                stringRedisTemplate,
                circuitBreakerRegistry
            ),
            jdbcTemplate,
            registeredClientRepository
        )

    @Bean
    fun authorizationConsentService(
        jdbcTemplate: JdbcTemplate,
        registeredClientRepository: RegisteredClientRepository
    ): OAuth2AuthorizationConsentService =
        JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository)

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val (keyPair, keyId) = if (rsaKeyPath.isNotBlank()) {
            loadRsaKey() to "rsa-key"          // 고정 ID → 재기동 후에도 기존 토큰 유효
        } else {
            generateRsaKey() to UUID.randomUUID().toString()  // 로컬 개발용 인메모리
        }
        val rsaKey = RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private as RSAPrivateKey)
            .keyID(keyId)
            .build()
        return ImmutableJWKSet(JWKSet(rsaKey))
    }

    private fun loadRsaKey(): KeyPair {
        val privateKey = File("$rsaKeyPath/private.pem").inputStream().use {
            RsaKeyConverters.pkcs8().convert(it)
        }
        val publicKey = File("$rsaKeyPath/public.pem").inputStream().use {
            RsaKeyConverters.x509().convert(it)
        }
        return KeyPair(requireNotNull(publicKey), requireNotNull(privateKey))
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder =
        OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings =
        AuthorizationServerSettings.builder()
            .issuer(issuer)
            .build()

    private fun generateRsaKey(): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
}
