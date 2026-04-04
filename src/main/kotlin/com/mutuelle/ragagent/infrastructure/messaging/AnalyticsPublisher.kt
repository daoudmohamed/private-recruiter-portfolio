package com.mutuelle.ragagent.infrastructure.messaging

import com.mutuelle.ragagent.config.RabbitMqConfig
import com.mutuelle.ragagent.domain.event.ConversationAnalyticsEvent
import com.mutuelle.ragagent.domain.event.DomainEvent
import mu.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Publisher for analytics events to RabbitMQ.
 */
@Component
class AnalyticsPublisher(
    private val rabbitTemplate: RabbitTemplate
) {
    /**
     * Publishes an analytics event.
     */
    fun publish(event: DomainEvent) {
        logger.debug { "Publishing analytics event: ${event.eventId}" }

        try {
            rabbitTemplate.convertAndSend(
                RabbitMqConfig.ANALYTICS_EXCHANGE,
                RabbitMqConfig.ANALYTICS_ROUTING_KEY,
                event
            )
        } catch (e: Exception) {
            // Analytics failures should not break the main flow
            logger.warn(e) { "Failed to publish analytics event: ${event.eventId}" }
        }
    }

    /**
     * Publishes a conversation analytics event.
     */
    fun publishConversationAnalytics(event: ConversationAnalyticsEvent) {
        publish(event)
    }
}
