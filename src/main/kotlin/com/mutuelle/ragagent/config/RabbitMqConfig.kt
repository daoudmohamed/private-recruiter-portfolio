package com.mutuelle.ragagent.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for RabbitMQ messaging.
 */
@Configuration
class RabbitMqConfig {

    @Value("\${mutuelle.escalation.queue-name:escalation.queue}")
    private lateinit var escalationQueueName: String

    companion object {
        const val ESCALATION_EXCHANGE = "escalation.exchange"
        const val ESCALATION_ROUTING_KEY = "escalation.new"

        const val DOCUMENT_GENERATION_QUEUE = "document.generation.queue"
        const val DOCUMENT_GENERATION_EXCHANGE = "document.exchange"
        const val DOCUMENT_GENERATION_ROUTING_KEY = "document.generate"

        const val ANALYTICS_QUEUE = "analytics.queue"
        const val ANALYTICS_EXCHANGE = "analytics.exchange"
        const val ANALYTICS_ROUTING_KEY = "analytics.event"
    }

    // ==================== Escalation Queue ====================

    @Bean
    fun escalationQueue(): Queue {
        return QueueBuilder.durable(escalationQueueName)
            .withArgument("x-message-ttl", 86400000) // 24 hours TTL
            .build()
    }

    @Bean
    fun escalationExchange(): TopicExchange {
        return TopicExchange(ESCALATION_EXCHANGE)
    }

    @Bean
    fun escalationBinding(
        escalationQueue: Queue,
        escalationExchange: TopicExchange
    ): Binding {
        return BindingBuilder
            .bind(escalationQueue)
            .to(escalationExchange)
            .with("escalation.#")
    }

    // ==================== Document Generation Queue ====================

    @Bean
    fun documentGenerationQueue(): Queue {
        return QueueBuilder.durable(DOCUMENT_GENERATION_QUEUE)
            .build()
    }

    @Bean
    fun documentExchange(): TopicExchange {
        return TopicExchange(DOCUMENT_GENERATION_EXCHANGE)
    }

    @Bean
    fun documentBinding(
        documentGenerationQueue: Queue,
        documentExchange: TopicExchange
    ): Binding {
        return BindingBuilder
            .bind(documentGenerationQueue)
            .to(documentExchange)
            .with("document.#")
    }

    // ==================== Analytics Queue ====================

    @Bean
    fun analyticsQueue(): Queue {
        return QueueBuilder.durable(ANALYTICS_QUEUE)
            .build()
    }

    @Bean
    fun analyticsExchange(): TopicExchange {
        return TopicExchange(ANALYTICS_EXCHANGE)
    }

    @Bean
    fun analyticsBinding(
        analyticsQueue: Queue,
        analyticsExchange: TopicExchange
    ): Binding {
        return BindingBuilder
            .bind(analyticsQueue)
            .to(analyticsExchange)
            .with("analytics.#")
    }

    // ==================== Message Converter ====================

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: MessageConverter
    ): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        return template
    }
}
