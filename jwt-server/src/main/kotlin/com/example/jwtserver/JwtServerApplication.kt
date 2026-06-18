package com.example.jwtserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class JwtServerApplication

fun main(args: Array<String>) {
    runApplication<JwtServerApplication>(*args)
}
