package com.example.eurekaserver

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.ignoringRequestMatchers("/eureka/**") }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health").permitAll()
                it.anyRequest().authenticated()
            }
            .httpBasic { }
            .formLogin { }
        return http.build()
    }
}
