package com.example.authorizationserver

import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class PkceGeneratorTest {

    @Test
    fun generatePkce() {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        println("code_verifier  : $codeVerifier")
        println("code_challenge : $codeChallenge")
        println()
        println("GET http://localhost:1010/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=jwt-service-client" +
                "&redirect_uri=http://localhost:1020/login/oauth2/code/jwt-service-client" +
                "&scope=openid" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256")
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

}
