package com.mutuelle.ragagent.infrastructure.messaging

import com.mutuelle.ragagent.config.RabbitMqConfig
import com.mutuelle.ragagent.domain.event.EscalationEvent
import mu.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Publisher for escalation events to RabbitMQ.
 */
@Component
class EscalationPublisher(
    private val rabbitTemplate: RabbitTemplate
) {
    /**
     * Publishes an escalation event to the queue.
     */
    fun publish(event: EscalationEvent) {
        logger.info { "Publishing escalation event for session: ${event.sessionId}" }

        try {
            rabbitTemplate.convertAndSend(
                RabbitMqConfig.ESCALATION_EXCHANGE,
                RabbitMqConfig.ESCALATION_ROUTING_KEY,
                event
            )
            logger.debug { "Escalation event published successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish escalation event for session: ${event.sessionId}" }
            throw e
        }
    }
}
