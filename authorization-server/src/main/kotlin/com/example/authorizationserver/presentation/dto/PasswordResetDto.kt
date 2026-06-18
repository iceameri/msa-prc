package com.example.authorizationserver.presentation.dto

data class ResetRequestBody(val email: String)
data class ResetConfirmBody(val token: String, val newPassword: String)
