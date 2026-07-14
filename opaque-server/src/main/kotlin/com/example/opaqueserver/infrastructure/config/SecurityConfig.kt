package com.example.opaqueserver.infrastructure.config

import com.example.opaqueserver.infrastructure.ratelimit.OpenApiRateLimitFilter
import com.example.opaqueserver.infrastructure.security.CustomOpaqueTokenIntrospector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.http.HttpMethod
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val introspector: CustomOpaqueTokenIntrospector,
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {

    @Bean
    @Order(1)
    fun openApiFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.securityMatcher("/openapi/**")
        http.cors { it.configurationSource(corsConfigurationSource()) }
        http.csrf { it.disable() }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.authorizeHttpRequests { it.anyRequest().authenticated() }
        http.oauth2ResourceServer { it.jwt {} }
        http.addFilterAfter(
            OpenApiRateLimitFilter(redis, objectMapper),
            UsernamePasswordAuthenticationFilter::class.java
        )
        return http.build()
    }

    @Bean
    @Order(2)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.cors { it.configurationSource(corsConfigurationSource()) }
        http.csrf { it.disable() }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.authorizeHttpRequests {
            it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            it.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            it.requestMatchers("/admin/**").hasAnyRole("ADMIN", "SYSTEM")
            it.requestMatchers("/payments/**").hasAnyRole("USER", "ADMIN")
            it.anyRequest().authenticated()
        }
        http.oauth2ResourceServer {
            it.opaqueToken { opaque -> opaque.introspector(introspector) }
        }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOriginPatterns = listOf("*")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.maxAge = 3600L
        return UrlBasedCorsConfigurationSource().also {
            it.registerCorsConfiguration("/**", config)
        }
    }
}
