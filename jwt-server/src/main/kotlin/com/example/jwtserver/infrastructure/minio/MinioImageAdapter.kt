package com.example.jwtserver.infrastructure.minio

import com.example.jwtserver.application.port.out.ImageStoragePort
import com.example.jwtserver.infrastructure.config.MinioProperties
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class MinioImageAdapter(
    private val minioClient: MinioClient,
    private val properties: MinioProperties
) : ImageStoragePort {

    override fun upload(filename: String, contentType: String, size: Long, stream: InputStream): String {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(properties.bucketName)
                .`object`(filename)
                .contentType(contentType)
                .stream(stream, size, -1)
                .build()
        )
        return "${properties.endpoint}/${properties.bucketName}/$filename"
    }

    override fun delete(objectName: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(properties.bucketName)
                .`object`(objectName)
                .build()
        )
    }
}
