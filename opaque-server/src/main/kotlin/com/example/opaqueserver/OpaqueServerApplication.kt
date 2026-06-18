package com.example.opaqueserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpaqueServerApplication

fun main(args: Array<String>) {
    runApplication<OpaqueServerApplication>(*args)
}
