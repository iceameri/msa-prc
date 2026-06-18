package com.example.authorizationserver.infrastructure.oauth2

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class TokenRevocationService(private val jdbcTemplate: JdbcTemplate) {

    fun revokeAllForPrincipal(principalName: String) {
        jdbcTemplate.update(
            "DELETE FROM oauth2_authorization WHERE principal_name = ?",
            principalName
        )
    }
}
