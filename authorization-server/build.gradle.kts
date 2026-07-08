plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "authorization-server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // OAuth2 Authorization Server (OAuth 2.1, OIDC)
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server")

    // Social Login
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    // Redis (토큰/세션 캐시)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kafka (이벤트 발행)
    implementation("org.springframework.kafka:spring-kafka")

    // Mail (비밀번호 재설정)
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // QR 로그인
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // MFA / TOTP
    implementation("dev.samstevens.totp:totp:1.7.1")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Resilience4j (circuit breaker, retry, timeout) + AOP
    implementation("io.github.resilience4j:resilience4j-spring-boot4:2.4.0")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    // Tracing - OpenTelemetry (LGTM)
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // Prometheus Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Loki (로그 수집)
    implementation("com.github.loki4j:loki-logback-appender:1.5.2")

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

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    imageName.set("iceameri/${project.description}:${project.version}")
}
