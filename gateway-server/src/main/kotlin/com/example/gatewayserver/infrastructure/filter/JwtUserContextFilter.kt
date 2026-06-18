package com.example.gatewayserver.infrastructure.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.data.redis.core.ReactiveRedisTemplate
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
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> =
        ReactiveSecurityContextHolder.getContext()
            .mapNotNull { it.authentication }
            .filter { it.isAuthenticated }
            .flatMap { auth ->
                // sub = userId string (after OAuth2TokenCustomizer), username claim added separately
                val jwtAuth = auth as? JwtAuthenticationToken
                val userId = jwtAuth?.token?.getClaim<String>("user_id") ?: jwtAuth?.token?.subject ?: auth.name
                val username = jwtAuth?.token?.getClaim<String>("username") ?: userId

                redisTemplate.opsForValue().get("$AUTHORITIES_KEY_PREFIX$username")
                    .map { value ->
                        @Suppress("UNCHECKED_CAST")
                        val authorities = (value as? Collection<*>)
                            ?.filterIsInstance<String>()
                            ?.joinToString(",")
                            ?: ""
                        mutateExchange(exchange, userId, username, authorities)
                    }
                    .defaultIfEmpty(mutateExchange(exchange, userId, username, ""))
            }
            .defaultIfEmpty(exchange)
            .flatMap { chain.filter(it) }

    private fun mutateExchange(
        exchange: ServerWebExchange,
        userId: String,
        username: String,
        authorities: String
    ): ServerWebExchange =
        exchange.mutate()
            .request(
                exchange.request.mutate()
                    .header(HEADER_USER_ID, userId)
                    .header(HEADER_USER_NAME, username)
                    .header(HEADER_USER_AUTHORITIES, authorities)
                    .build()
            )
            .build()

    // Spring Security WebFilter는 WebFilter 체인(-100)에서 처리되고
    // GlobalFilter는 그 이후 Gateway 체인에서 실행되므로 SecurityContext 접근 가능
    override fun getOrder(): Int = 0
}
