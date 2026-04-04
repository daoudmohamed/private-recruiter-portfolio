package com.mutuelle.ragagent.application.service

import com.mutuelle.ragagent.config.RabbitMqConfig
import com.mutuelle.ragagent.domain.event.EscalationEvent
import com.mutuelle.ragagent.domain.event.EscalationPriority
import com.mutuelle.ragagent.domain.event.EscalationResolvedEvent
import mu.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Service for managing escalations to human agents.
 */
@Service
class EscalationService(
    private val rabbitTemplate: RabbitTemplate,
    private val sessionService: SessionService
) {
    /**
     * Escalates a conversation to a human agent.
     */
    suspend fun escalate(
        sessionId: String,
        adherentId: UUID,
        lastMessage: String,
        reason: String,
        priority: EscalationPriority = EscalationPriority.NORMAL,
        conversationSummary: String? = null
    ) {
        logger.info { "Escalating session: $sessionId, reason: $reason" }

        val event = EscalationEvent(
            adherentId = adherentId,
            sessionId = sessionId,
            reason = reason,
            lastMessage = lastMessage,
            conversationSummary = conversationSummary,
            priority = priority
        )

        // Publish to RabbitMQ
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.ESCALATION_EXCHANGE,
            RabbitMqConfig.ESCALATION_ROUTING_KEY,
            event
        )

        // Update session status
        sessionService.escalateSession(sessionId, reason)

        logger.info { "Escalation event published for session: $sessionId" }
    }

    /**
     * Marks an escalation as resolved.
     */
    suspend fun markResolved(
        sessionId: String,
        resolution: String,
        resolvedBy: String
    ) {
        logger.info { "Marking escalation resolved for session: $sessionId" }

        val session = sessionService.getSession(sessionId)
        if (session == null) {
            logger.warn { "Session not found for resolution: $sessionId" }
            return
        }

        val resolutionTime = session.escalatedAt?.let {
            java.time.Duration.between(it, Instant.now()).toMinutes()
        } ?: 0L

        val event = EscalationResolvedEvent(
            adherentId = session.adherentId,
            sessionId = sessionId,
            resolution = resolution,
            resolvedBy = resolvedBy,
            resolutionTimeMinutes = resolutionTime
        )

        // Publish resolution event for analytics
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.ANALYTICS_EXCHANGE,
            RabbitMqConfig.ANALYTICS_ROUTING_KEY,
            event
        )

        // Close the session
        sessionService.closeSession(sessionId)

        logger.info { "Escalation resolved for session: $sessionId in ${resolutionTime} minutes" }
    }

    /**
     * Gets pending escalations count (for monitoring).
     */
    fun getPendingEscalationsCount(): Int {
        // This would query the queue or a database
        // Simplified implementation
        return 0
    }
}
