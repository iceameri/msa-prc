package com.example.jwtserver.application.port.out

interface AuthoritiesCachePort {
    fun getAuthorities(userId: Long): Set<String>?
}
