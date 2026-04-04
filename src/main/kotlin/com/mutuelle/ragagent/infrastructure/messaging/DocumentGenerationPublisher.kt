package com.mutuelle.ragagent.infrastructure.messaging

import com.mutuelle.ragagent.config.RabbitMqConfig
import com.mutuelle.ragagent.domain.event.DocumentRequestedEvent
import mu.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Publisher for document generation requests to RabbitMQ.
 */
@Component
class DocumentGenerationPublisher(
    private val rabbitTemplate: RabbitTemplate
) {
    /**
     * Publishes a document generation request.
     */
    fun publish(event: DocumentRequestedEvent) {
        logger.info { "Publishing document generation request for adherent: ${event.adherentId}" }

        try {
            rabbitTemplate.convertAndSend(
                RabbitMqConfig.DOCUMENT_GENERATION_EXCHANGE,
                RabbitMqConfig.DOCUMENT_GENERATION_ROUTING_KEY,
                event
            )
            logger.debug { "Document generation request published successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish document generation request" }
            throw e
        }
    }
}
