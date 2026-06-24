package com.example.jwtserver.domain.user

import java.time.Instant

data class User(
    val id: Long,
    val username: String,
    val createdAt: Instant,
    // 이벤트 발행 시각 — 순서 역전된 메시지 거부의 비교 기준
    val updatedAt: Instant
)
