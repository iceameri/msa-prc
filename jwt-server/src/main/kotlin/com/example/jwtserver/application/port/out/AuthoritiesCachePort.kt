package com.example.jwtserver.application.port.out

interface AuthoritiesCachePort {
    fun getAuthorities(username: String): Set<String>?
}
