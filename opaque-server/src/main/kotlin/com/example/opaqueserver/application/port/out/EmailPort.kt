package com.example.opaqueserver.application.port.out

interface EmailPort {
    fun send(to: String, subject: String, body: String)
}
