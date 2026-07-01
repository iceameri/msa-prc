package com.example.gatewayserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

// DEPRECATED: Istio IngressGateway + K8s Gateway API(HTTPRoute)가 대체합니다.
// 라우팅: /auth/** → authorization-server, /api/** → jwt-server,
//         /admin/** → opaque-server, /payments/** → opaque-server
// Rate Limiting: 각 서비스 자체 처리(bucket4j + Redis)로 이전 예정
// CORS: HTTPRoute 또는 각 서비스 자체 처리로 이전 예정
@Deprecated("Replaced by Istio IngressGateway + Kubernetes Gateway API (HTTPRoute)")
@SpringBootApplication
class GatewayServerApplication

fun main(args: Array<String>) {
    error(
        """
        [DEPRECATED] gateway-server는 더 이상 사용되지 않습니다.
        Istio IngressGateway + K8s Gateway API(HTTPRoute)가 대체합니다.
        로컬 개발 시에만 -Dlegacy.gateway.enabled=true 플래그로 실행하세요.
        """.trimIndent()
    )
}
