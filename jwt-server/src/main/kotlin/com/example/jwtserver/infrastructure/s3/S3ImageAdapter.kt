package com.example.jwtserver.infrastructure.s3

import com.example.jwtserver.application.port.out.ImageStoragePort
import com.example.jwtserver.infrastructure.config.S3Properties
// import org.springframework.stereotype.Component
// import software.amazon.awssdk.core.sync.RequestBody
// import software.amazon.awssdk.services.s3.S3Client
// import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
// import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream

// @Component
class S3ImageAdapter(
    private val s3Client: Any, // S3Client
    private val properties: S3Properties
) : ImageStoragePort {

    override fun upload(filename: String, contentType: String, size: Long, stream: InputStream): String {
        // minioClient.putObject(
        //     PutObjectArgs.builder()
        //         .bucket(properties.bucketName)
        //         .`object`(filename)
        //         .contentType(contentType)
        //         .stream(stream, size, -1)
        //         .build()
        // )
        // return "${properties.endpoint}/${properties.bucketName}/$filename"

        // s3Client.putObject(
        //     PutObjectRequest.builder()
        //         .bucket(properties.bucketName)
        //         .key(filename)
        //         .contentType(contentType)
        //         .build(),
        //     RequestBody.fromInputStream(stream, size)
        // )
        // return "https://${properties.bucketName}.s3.${properties.region}.amazonaws.com/$filename"
        throw UnsupportedOperationException("S3 not configured")
    }

    override fun delete(objectName: String) {
        // minioClient.removeObject(
        //     RemoveObjectArgs.builder()
        //         .bucket(properties.bucketName)
        //         .`object`(objectName)
        //         .build()
        // )

        // s3Client.deleteObject(
        //     DeleteObjectRequest.builder()
        //         .bucket(properties.bucketName)
        //         .key(objectName)
        //         .build()
        // )
        throw UnsupportedOperationException("S3 not configured")
    }
}
