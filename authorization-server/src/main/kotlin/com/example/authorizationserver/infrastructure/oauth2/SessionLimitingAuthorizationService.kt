package com.example.authorizationserver.infrastructure.oauth2

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

class SessionLimitingAuthorizationService(
    private val delegate: OAuth2AuthorizationService,
    private val jdbcTemplate: JdbcTemplate,
    private val registeredClientRepository: RegisteredClientRepository
) : OAuth2AuthorizationService {

    companion object {
        private const val OPAQUE_CLIENT_ID = "opaque-server-client"
        private const val MAX_SESSIONS = 5
    }

    override fun save(authorization: OAuth2Authorization) {
        val clientId = registeredClientRepository.findById(authorization.registeredClientId)?.clientId
        if (clientId == OPAQUE_CLIENT_ID) {
            enforceSessionLimit(authorization.principalName, authorization.registeredClientId)
        }
        delegate.save(authorization)
    }

    override fun remove(authorization: OAuth2Authorization) = delegate.remove(authorization)

    override fun findById(id: String): OAuth2Authorization? = delegate.findById(id)

    override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? =
        delegate.findByToken(token, tokenType)

    private fun enforceSessionLimit(principalName: String, registeredClientId: String) {
        val ids = jdbcTemplate.queryForList<String>(
            """
            SELECT  id
            FROM    authorization_db.public.oauth2_authorization
            WHERE   principal_name = ? AND registered_client_id = ?
            ORDER BY COALESCE(access_token_issued_at, authorization_code_issued_at)
            """.trimMargin(),
            principalName, registeredClientId
        )
        if (ids.size >= MAX_SESSIONS) {
            ids.take(ids.size - MAX_SESSIONS + 1).forEach { id ->
                delegate.findById(id)?.let { delegate.remove(it) }
            }
        }
    }
}
