package com.example.jwtserver.application.service

import com.example.jwtserver.application.port.out.ImageStoragePort
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class ImageService(private val imageStoragePort: ImageStoragePort) {

    fun upload(file: MultipartFile): String {
        val ext = file.originalFilename?.substringAfterLast('.', "") ?: "bin"
        val objectName = "posts/${UUID.randomUUID()}.$ext"
        return imageStoragePort.upload(
            filename = objectName,
            contentType = file.contentType ?: "application/octet-stream",
            size = file.size,
            stream = file.inputStream
        )
    }
}
