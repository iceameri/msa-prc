package com.example.authorizationserver.domain.qr

data class QrLoginSession(
    val token: String,
    val status: QrLoginStatus,
    val username: String? = null
)

enum class QrLoginStatus { PENDING, CONFIRMED, EXPIRED }
