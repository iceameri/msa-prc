package com.example.gatewayserver.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator

@Configuration
class ReactiveRedisConfig {

    @Bean
    fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Any> {
        val typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(Any::class.java)
            .build()
        val valueSerializer = GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(typeValidator)
            .build()
        val context = RedisSerializationContext
            .newSerializationContext<String, Any>(StringRedisSerializer())
            .value(valueSerializer)
            .build()
        return ReactiveRedisTemplate(factory, context)
    }
}
