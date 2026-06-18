package com.example.authorizationserver.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

@Configuration
class RedisConfig {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(Any::class.java)
            .build()
        val serializer = GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(typeValidator)
            .build()
        return RedisTemplate<String, Any>().apply {
            this.connectionFactory = connectionFactory
            keySerializer = StringRedisSerializer()
            valueSerializer = serializer
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = serializer
        }
    }
}
