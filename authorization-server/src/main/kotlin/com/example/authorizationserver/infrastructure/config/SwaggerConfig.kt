package com.example.authorizationserver.infrastructure.config

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
                .title("Authorization Server API")
                .description("OAuth 2.1 기반 인증 서버 - 토큰 발급, OIDC, QR 로그인")
                .version("1.0.0")
        )
}
