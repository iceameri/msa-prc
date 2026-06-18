package com.example.jwtserver.infrastructure.config

import com.example.jwtserver.infrastructure.security.GatewayHeaderAuthenticationFilter
import jakarta.servlet.DispatcherType
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    // private val customJwtAuthenticationConverter: CustomJwtAuthenticationConverter
    private val gatewayHeaderAuthenticationFilter: GatewayHeaderAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }
        http.authorizeHttpRequests {
            it.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            it.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            it.anyRequest().authenticated()
        }
//        http.oauth2ResourceServer {
//            it.jwt { jwt ->
//                jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)
//            }
//        }
        http.addFilterBefore(gatewayHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    // @Component로 인한 서블릿 레벨 자동 등록 방지
    // (Security 체인 외부에서 먼저 실행되면 OncePerRequestFilter 중복 방지로 내부에서 스킵됨)
    @Bean
    fun disableGatewayHeaderFilterAutoRegistration(
        filter: GatewayHeaderAuthenticationFilter
    ): FilterRegistrationBean<GatewayHeaderAuthenticationFilter> =
        FilterRegistrationBean(filter).also { it.isEnabled = false }
}
