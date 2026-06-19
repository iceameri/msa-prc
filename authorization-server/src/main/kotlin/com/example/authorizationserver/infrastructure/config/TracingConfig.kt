package com.example.authorizationserver.infrastructure.config

import brave.http.HttpClientSampler
import brave.http.HttpRequest
import brave.sampler.SamplerFunction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TracingConfig {

    @Bean(name = [HttpClientSampler.NAME])
    fun httpClientSampler(): SamplerFunction<HttpRequest> =
        SamplerFunction { request -> if (request.path().contains("/eureka/")) false else null }
}
