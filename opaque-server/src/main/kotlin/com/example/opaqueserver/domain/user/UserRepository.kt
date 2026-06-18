package com.example.opaqueserver.domain.user

interface UserRepository {
    fun findById(id: String): User?
}
