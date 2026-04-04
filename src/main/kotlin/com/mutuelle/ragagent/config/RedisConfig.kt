package com.mutuelle.ragagent.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.context.annotation.Primary
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisIndexedWebSession

/**
 * Configuration for Redis (cache, sessions, chat memory).
 */
@Configuration
@EnableRedisIndexedWebSession(maxInactiveIntervalInSeconds = 86400) // 24 hours
class RedisConfig {

    @Value("\${spring.data.redis.host:localhost}")
    private lateinit var host: String

    @Value("\${spring.data.redis.port:6379}")
    private var port: Int = 6379

    @Value("\${spring.data.redis.password:}")
    private var password: String? = null

    /**
     * Creates a reactive Redis connection factory.
     */
    @Bean
    @Primary
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val config = RedisStandaloneConfiguration(host, port)
        if (!password.isNullOrBlank()) {
            config.setPassword(password)
        }
        return LettuceConnectionFactory(config)
    }

    /**
     * Creates a reactive Redis template for string operations.
     */
    @Bean("reactiveStringRedisTemplate")
    fun reactiveStringRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveRedisTemplate<String, String> {
        val serializer = StringRedisSerializer()
        val context = RedisSerializationContext
            .newSerializationContext<String, String>(serializer)
            .key(serializer)
            .value(serializer)
            .hashKey(serializer)
            .hashValue(serializer)
            .build()

        return ReactiveRedisTemplate(connectionFactory, context)
    }

    /**
     * Creates a reactive Redis template for JSON object operations.
     */
    @Bean
    fun reactiveJsonRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory,
        objectMapper: ObjectMapper
    ): ReactiveRedisTemplate<String, Any> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(objectMapper, Any::class.java)

        val context = RedisSerializationContext
            .newSerializationContext<String, Any>(keySerializer)
            .key(keySerializer)
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build()

        return ReactiveRedisTemplate(connectionFactory, context)
    }

    /**
     * Configures ObjectMapper for Redis serialization.
     */
    @Bean
    fun redisObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
        }
    }
}
