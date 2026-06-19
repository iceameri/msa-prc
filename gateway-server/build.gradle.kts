plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "gateway-server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.1.1"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Spring Cloud Gateway (WebFlux 기반 - spring-boot-starter-web과 함께 사용 불가)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    // JWT 사전 검증
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Redis (Rate limiting)
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Resilience4j Reactive (circuit breaker)
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    // Tracing - Brave + Zipkin (Spring Boot 4.0: spring-boot-starter-zipkin으로 통합)
    implementation("org.springframework.boot:spring-boot-starter-zipkin")
    implementation("io.zipkin.brave:brave")

    // Service Discovery & Config
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-config")

    // Swagger (WebFlux)
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3")

    // Prometheus Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logstash (ELK)
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
