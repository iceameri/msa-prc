package com.example.jwtserver.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "s3")
data class S3Properties(
    val region: String,
    val accessKey: String,
    val secretKey: String,
    val bucketName: String
)
