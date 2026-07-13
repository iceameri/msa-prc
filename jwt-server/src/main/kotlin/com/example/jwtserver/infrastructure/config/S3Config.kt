package com.example.jwtserver.infrastructure.config

// import org.springframework.boot.context.properties.EnableConfigurationProperties
// import org.springframework.context.annotation.Bean
// import org.springframework.context.annotation.Configuration
// import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
// import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
// import software.amazon.awssdk.regions.Region
// import software.amazon.awssdk.services.s3.S3Client

// @Configuration
// @EnableConfigurationProperties(S3Properties::class)
// class S3Config {
//
//     @Bean
//     fun s3Client(properties: S3Properties): S3Client =
//         S3Client.builder()
//             .region(Region.of(properties.region))
//             .credentialsProvider(
//                 StaticCredentialsProvider.create(
//                     AwsBasicCredentials.create(properties.accessKey, properties.secretKey)
//                 )
//             )
//             .build()
// }
