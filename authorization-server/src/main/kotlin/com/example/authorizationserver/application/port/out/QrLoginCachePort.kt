package com.example.authorizationserver.application.port.out

import com.example.authorizationserver.domain.qr.QrLoginSession

interface QrLoginCachePort {
    fun save(session: QrLoginSession)
    fun get(token: String): QrLoginSession?
    fun confirm(token: String, username: String)
    fun delete(token: String)
}
