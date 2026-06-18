package com.example.gatewayserver.infrastructure.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class LoggingFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val start = System.currentTimeMillis()
        log.info("→ {} {}", request.method, request.uri.path)
        return chain.filter(exchange).doFinally {
            val status = exchange.response.statusCode?.value() ?: 0
            log.info("← {} {} {}ms", status, request.uri.path, System.currentTimeMillis() - start)
        }
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}
