package com.example.jwtserver.infrastructure.s3

import com.example.jwtserver.application.port.out.ImageStoragePort
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class NoOpImageAdapter : ImageStoragePort {

    override fun upload(filename: String, contentType: String, size: Long, stream: InputStream): String {
        throw UnsupportedOperationException("Image storage not configured — enable S3ImageAdapter")
    }

    override fun delete(objectName: String) {
        throw UnsupportedOperationException("Image storage not configured — enable S3ImageAdapter")
    }
}
