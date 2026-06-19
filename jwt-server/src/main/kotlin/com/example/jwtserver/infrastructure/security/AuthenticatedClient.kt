package com.example.jwtserver.infrastructure.security

data class AuthenticatedClient(val clientId: String) : CallerPrincipal
