package com.example.authorizationserver.application.service

import com.example.authorizationserver.application.port.out.UserCachePort
import com.example.authorizationserver.infrastructure.persistence.UserJdbcRepository
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class MfaService(
    private val userRepository: UserJdbcRepository,
    private val userCachePort: UserCachePort
) {
    private val secretGenerator = DefaultSecretGenerator()
    private val codeVerifier = DefaultCodeVerifier(DefaultCodeGenerator(), SystemTimeProvider())
    private val qrGenerator = ZxingPngQrGenerator()

    fun generateSetup(username: String): MfaSetupData {
        val secret = secretGenerator.generate()
        userCachePort.savePendingMfaSecret(username, secret)

        val qrData = QrData.Builder()
            .label(username)
            .secret(secret)
            .issuer("msa-prc")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build()

        val qrBytes = qrGenerator.generate(qrData)
        return MfaSetupData(
            secret = secret,
            qrCodeBase64 = Base64.getEncoder().encodeToString(qrBytes)
        )
    }

    fun enableMfa(username: String, code: String): Boolean {
        val secret = userCachePort.getPendingMfaSecret(username) ?: return false
        if (!codeVerifier.isValidCode(secret, code)) return false

        userRepository.updateMfaSettings(username, true, secret)
        userCachePort.deletePendingMfaSecret(username)
        userCachePort.deleteUser(username)
        return true
    }

    fun disableMfa(username: String) {
        userRepository.updateMfaSettings(username, false, null)
        userCachePort.deleteUser(username)
    }

    fun verifyCode(secret: String, code: String): Boolean =
        codeVerifier.isValidCode(secret, code)
}

data class MfaSetupData(val secret: String, val qrCodeBase64: String)
