package com.example.opaqueserver.infrastructure.rest

import com.example.opaqueserver.domain.user.User
import com.example.opaqueserver.domain.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class UserRestRepository(
    @Value("\${service.authorization.url:http://localhost:9000}") private val baseUrl: String
) : UserRepository {

    private val log = LoggerFactory.getLogger(UserRestRepository::class.java)
    private val restClient = RestClient.builder().baseUrl(baseUrl).build()

    override fun findById(id: String): User? = try {
        val body = restClient.get()
            .uri("/internal/users/{id}", id)
            .retrieve()
            .body(object : ParameterizedTypeReference<Map<String, Any?>>() {})
        body?.let {
            User(
                id = it["id"]?.toString() ?: id,
                username = it["username"]?.toString() ?: "",
                email = it["email"]?.toString()
            )
        }
    } catch (ex: Exception) {
        log.debug("Could not fetch user info for id={}: {}", id, ex.message)
        null
    }
}
