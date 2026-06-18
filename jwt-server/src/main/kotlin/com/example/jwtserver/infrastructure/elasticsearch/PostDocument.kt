package com.example.jwtserver.infrastructure.elasticsearch

data class PostDocument(
    val id: String,
    val authorUsername: String,
    val title: String,
    val content: String,
    val hashtags: List<String>,
    val createdAt: String
)

data class UserDocument(
    val username: String
)
