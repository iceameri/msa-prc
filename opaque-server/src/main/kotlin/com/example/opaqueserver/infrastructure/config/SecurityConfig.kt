package com.example.opaqueserver.infrastructure.config

import com.example.opaqueserver.infrastructure.security.CustomOpaqueTokenIntrospector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(private val introspector: CustomOpaqueTokenIntrospector) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.authorizeHttpRequests {
            it.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            it.requestMatchers("/admin/**").hasRole("ADMIN")
            it.requestMatchers("/payments/**").hasAnyRole("USER", "ADMIN")
            it.anyRequest().authenticated()
        }
        http.oauth2ResourceServer {
            it.opaqueToken { opaque -> opaque.introspector(introspector) }
        }
        return http.build()
    }
}
