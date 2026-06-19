package com.example.jwtserver.infrastructure.security

data class AuthenticatedUser(val id: Long, val username: String, val roles: List<String> = emptyList()) : CallerPrincipal
