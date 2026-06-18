package com.example.authorizationserver.presentation.dto

data class MfaSetupResponse(val qrCodeBase64: String)
data class MfaCodeRequest(val code: String)
