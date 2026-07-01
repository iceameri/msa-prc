package com.example.eurekaserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

// DEPRECATED: K8s + CoreDNS가 서비스 디스커버리를 대체합니다.
// 이 서버는 K8s 환경에서 실행하지 마세요.
@Deprecated("Replaced by Kubernetes Service + CoreDNS")
@SpringBootApplication
@EnableEurekaServer
class EurekaServerApplication

fun main(args: Array<String>) {
    error(
        """
        [DEPRECATED] eureka-server는 더 이상 사용되지 않습니다.
        K8s + CoreDNS가 서비스 디스커버리를 대체합니다.
        로컬 개발 시에만 -Dlegacy.eureka.enabled=true 플래그로 실행하세요.
        """.trimIndent()
    )
}
