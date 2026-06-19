package com.example.gatewayserver.infrastructure.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtUserContextFilter(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>
) : GlobalFilter, Ordered {

    companion object {
        private const val AUTHORITIES_KEY_PREFIX = "jwt:authorities:"
        private const val HEADER_USER_ID = "X-User-Id"
        private const val HEADER_USER_NAME = "X-User-Name"
        private const val HEADER_USER_AUTHORITIES = "X-User-Authorities"
        private const val HEADER_CLIENT_ID = "X-Client-Id"
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> =
        ReactiveSecurityContextHolder.getContext()
            .mapNotNull { it.authentication }
            .filter { it.isAuthenticated }
            .flatMap { auth ->
                val jwtAuth = auth as? JwtAuthenticationToken
                val userId = jwtAuth?.token?.getClaim<String>("user_id")

                if (userId != null) {
                    val username = jwtAuth!!.token.getClaim<String>("username") ?: userId
                    redisTemplate.opsForValue().get("$AUTHORITIES_KEY_PREFIX$username")
                        .map { value ->
                            @Suppress("UNCHECKED_CAST")
                            val authorities = (value as? Collection<*>)
                                ?.filterIsInstance<String>()
                                ?.joinToString(",") ?: ""
                            buildUserExchange(exchange, userId, username, authorities)
                        }
                        .defaultIfEmpty(buildUserExchange(exchange, userId, username, ""))
                } else {
                    val clientId = jwtAuth?.token?.getClaim<String>("client_id")
                        ?: jwtAuth?.token?.subject ?: ""
                    Mono.just(buildClientExchange(exchange, clientId))
                }
            }
            .defaultIfEmpty(stripTrustedHeaders(exchange))
            .flatMap { chain.filter(it) }

    // Spring Security WebFilter는 WebFilter 체인(-100)에서 처리되고
    // GlobalFilter는 그 이후 Gateway 체인에서 실행되므로 SecurityContext 접근 가능
    override fun getOrder(): Int = 0

    private fun stripTrustedHeaders(exchange: ServerWebExchange): ServerWebExchange =
        exchange.mutate()
            .request(exchange.request.mutate().headers { it.removeTrustedHeaders() }.build())
            .build()

    private fun buildUserExchange(exchange: ServerWebExchange, userId: String, username: String, authorities: String): ServerWebExchange =
        exchange.mutate()
            .request(exchange.request.mutate()
                .headers {
                    it.removeTrustedHeaders()
                    it[HEADER_USER_ID] = listOf(userId)
                    it[HEADER_USER_NAME] = listOf(username)
                    it[HEADER_USER_AUTHORITIES] = listOf(authorities)
                }
                .build())
            .build()

    private fun buildClientExchange(exchange: ServerWebExchange, clientId: String): ServerWebExchange =
        exchange.mutate()
            .request(exchange.request.mutate()
                .headers {
                    it.removeTrustedHeaders()
                    it[HEADER_CLIENT_ID] = listOf(clientId)
                }
                .build())
            .build()

    private fun HttpHeaders.removeTrustedHeaders() {
        remove(HEADER_USER_ID)
        remove(HEADER_USER_NAME)
        remove(HEADER_USER_AUTHORITIES)
        remove(HEADER_CLIENT_ID)
    }
}
