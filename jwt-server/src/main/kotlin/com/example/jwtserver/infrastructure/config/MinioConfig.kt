package com.example.jwtserver.infrastructure.config

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MinioProperties::class)
class MinioConfig {

    @Bean
    fun minioClient(properties: MinioProperties): MinioClient {
        val client = MinioClient.builder()
            .endpoint(properties.endpoint)
            .credentials(properties.accessKey, properties.secretKey)
            .build()
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucketName).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucketName).build())
        }
        return client
    }
}
