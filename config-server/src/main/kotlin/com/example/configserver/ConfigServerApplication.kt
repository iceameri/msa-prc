package com.example.configserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer

// DEPRECATED: K8s ConfigMap + Secret이 설정 관리를 대체합니다.
// 설정 변경 자동 반영은 Stakater Reloader가 담당합니다.
// Spring Cloud Bus(Kafka)를 통한 /actuator/busrefresh도 함께 제거됩니다.
@Deprecated("Replaced by Kubernetes ConfigMap + Secret + Stakater Reloader")
@SpringBootApplication
@EnableConfigServer
class ConfigServerApplication

fun main(args: Array<String>) {
    error(
        """
        [DEPRECATED] config-server는 더 이상 사용되지 않습니다.
        K8s ConfigMap + Secret이 설정 관리를 대체합니다.
        설정 변경 자동 반영은 Stakater Reloader가 담당합니다.
        로컬 개발 시에만 -Dlegacy.config.enabled=true 플래그로 실행하세요.
        """.trimIndent()
    )
}
