package com.example.gatewayserver.infrastructure.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import java.security.Principal

@Configuration
class RateLimiterConfig {

    @Bean
    fun keyResolver(): KeyResolver = KeyResolver { exchange ->
        exchange.getPrincipal<Principal>()
            .map { it.name }
            .switchIfEmpty(
                Mono.justOrEmpty(
                    exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
                )
            )
    }
}
