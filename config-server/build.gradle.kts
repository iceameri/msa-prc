plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "config-server"

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

    // Web
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Config Server
    implementation("org.springframework.cloud:spring-cloud-config-server")

    // Spring Cloud Bus + Kafka (설정 변경 자동 반영)
    implementation("org.springframework.cloud:spring-cloud-starter-bus-kafka")

    // Actuator (refresh endpoint 포함)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Eureka (서비스 등록)
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

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
