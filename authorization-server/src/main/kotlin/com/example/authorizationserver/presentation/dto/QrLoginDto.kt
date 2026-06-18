package com.example.authorizationserver.presentation.dto

data class QrGenerateResponse(val token: String, val qrCodeBase64: String)
data class QrStatusResponse(val status: String)
data class QrConsumeResponse(val username: String)
