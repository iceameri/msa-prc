package com.example.authorizationserver.application.service

import com.example.authorizationserver.application.port.out.QrLoginCachePort
import com.example.authorizationserver.domain.qr.QrLoginSession
import com.example.authorizationserver.domain.qr.QrLoginStatus
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID

@Service
class QrLoginService(private val qrLoginCachePort: QrLoginCachePort) {

    fun generateQrSession(): QrGenerateResult {
        val token = UUID.randomUUID().toString()
        qrLoginCachePort.save(QrLoginSession(token = token, status = QrLoginStatus.PENDING))

        val qrContent = "msa-prc://qr-login?token=$token"
        val bitMatrix = QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, 200, 200)
        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
        val qrBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray())

        return QrGenerateResult(token = token, qrCodeBase64 = qrBase64)
    }

    fun getStatus(token: String): QrLoginStatus {
        val session = qrLoginCachePort.get(token) ?: return QrLoginStatus.EXPIRED
        return session.status
    }

    fun confirm(token: String, username: String): Boolean {
        val session = qrLoginCachePort.get(token) ?: return false
        if (session.status != QrLoginStatus.PENDING) return false
        qrLoginCachePort.confirm(token, username)
        return true
    }

    fun consumeConfirmedSession(token: String): String? {
        val session = qrLoginCachePort.get(token) ?: return null
        if (session.status != QrLoginStatus.CONFIRMED) return null
        qrLoginCachePort.delete(token)
        return session.username
    }
}

data class QrGenerateResult(val token: String, val qrCodeBase64: String)
