package com.mutuelle.ragagent.api.controller

import com.mutuelle.ragagent.application.dto.EscalationResolvedPayload
import com.mutuelle.ragagent.application.service.EscalationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

private val logger = KotlinLogging.logger {}

/**
 * REST controller for webhook callbacks.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Webhook callbacks for external systems")
class WebhookController(
    private val escalationService: EscalationService,
    @Value("\${mutuelle.webhook.secret:}")
    private val webhookSecret: String
) {
    /**
     * Callback when an escalation is resolved by a human agent.
     */
    @PostMapping("/escalation/resolved")
    @Operation(summary = "Webhook for escalation resolution")
    suspend fun escalationResolved(
        @RequestBody payload: EscalationResolvedPayload,
        @RequestHeader("X-Webhook-Secret") secret: String
    ): Map<String, String> {
        // Validate webhook secret
        if (webhookSecret.isNotBlank() && secret != webhookSecret) {
            logger.warn { "Invalid webhook secret received" }
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret")
        }

        logger.info { "Escalation resolved webhook received for session: ${payload.sessionId}" }

        escalationService.markResolved(
            sessionId = payload.sessionId,
            resolution = payload.resolution,
            resolvedBy = payload.resolvedBy
        )

        return mapOf(
            "status" to "accepted",
            "sessionId" to payload.sessionId
        )
    }

    /**
     * Health check endpoint for webhook monitoring.
     */
    @GetMapping("/health")
    @Operation(summary = "Webhook health check")
    fun webhookHealth(): Map<String, String> {
        return mapOf(
            "status" to "healthy",
            "service" to "mutuelle-rag-agent"
        )
    }
}
