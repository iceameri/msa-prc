package com.example.jwtserver.application.port.out

interface EventPublishPort {
    fun publish(topic: String, key: String, payload: String)
}
