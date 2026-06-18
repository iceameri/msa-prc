package com.example.jwtserver.infrastructure.config

import org.apache.hc.core5.http.EntityDetails
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.protocol.HttpContext
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticsearchCompatConfig {

    @Bean
    fun elasticsearchRest5ClientBuilderCustomizer(): Rest5ClientBuilderCustomizer =
        Rest5ClientBuilderCustomizer { builder ->
            builder.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.addRequestInterceptorLast { request: HttpRequest, _: EntityDetails?, _: HttpContext ->
                    // ES Java Client 9.x sends "compatible-with=9" but ES 8.x only accepts up to "compatible-with=8"
                    request.removeHeaders("Accept")
                    request.removeHeaders("Content-Type")
                    request.addHeader("Accept", "application/json")
                    request.addHeader("Content-Type", "application/json")
                }
                httpClientBuilder
            }
        }
}
