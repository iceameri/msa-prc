package com.example.jwtserver.infrastructure.config

import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.observation.ClientRequestObservationContext

@Configuration
class TracingConfig {

    @Bean
    fun eurekaTracingFilter(): ObservationPredicate =
        ObservationPredicate { _, context ->
            (context as? ClientRequestObservationContext)
                ?.carrier?.uri?.toString()
                ?.contains("/eureka/")
                ?.not() ?: true
        }
}
