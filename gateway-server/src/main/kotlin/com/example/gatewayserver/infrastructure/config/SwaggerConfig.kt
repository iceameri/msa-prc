package com.example.gatewayserver.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("MSA-PRC Gateway API")
                .version("1.0.0")
                .description("API Gateway — routes: /api/** → jwt-server, /admin/** → opaque-server, /auth/** → authorization-server")
        )
}
