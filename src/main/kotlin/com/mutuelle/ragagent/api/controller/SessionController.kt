package com.mutuelle.ragagent.api.controller

import com.mutuelle.ragagent.ai.memory.RedisChatMemory
import com.mutuelle.ragagent.application.dto.*
import com.mutuelle.ragagent.application.service.SessionService
import com.mutuelle.ragagent.domain.model.ConversationSession
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * REST controller for session management.
 */
@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Conversation session management")
class SessionController(
    private val sessionService: SessionService,
    private val chatMemory: RedisChatMemory
) {
    /**
     * Creates a new conversation session.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new conversation session")
    suspend fun createSession(
        @RequestBody(required = false) request: CreateSessionRequest?,
        @AuthenticationPrincipal jwt: Jwt
    ): SessionResponse {
        val adherentId = extractAdherentId(jwt)
        logger.info { "Creating session for adherent: $adherentId" }

        val session = sessionService.createSession(adherentId)
        return session.toResponse()
    }

    /**
     * Gets a session by ID.
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session details")
    suspend fun getSession(
        @PathVariable sessionId: String,
        @AuthenticationPrincipal jwt: Jwt
    ): SessionResponse {
        val session = sessionService.getSession(sessionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")

        // Verify ownership
        val adherentId = extractAdherentId(jwt)
        if (session.adherentId != adherentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        val messageCount = chatMemory.getMessageCount(sessionId).toInt()
        return session.toResponse(messageCount)
    }

    /**
     * Gets conversation history for a session.
     */
    @GetMapping("/{sessionId}/history")
    @Operation(summary = "Get conversation history")
    suspend fun getHistory(
        @PathVariable sessionId: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @AuthenticationPrincipal jwt: Jwt
    ): ChatHistoryResponse {
        val session = sessionService.getSession(sessionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")

        // Verify ownership
        val adherentId = extractAdherentId(jwt)
        if (session.adherentId != adherentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        val messages = chatMemory.getAsync(sessionId, limit)
        val messageDtos = messages.map { msg ->
            ChatMessageDto(
                role = msg.messageType.name,
                content = msg.text ?: "",
                timestamp = Instant.now() // Note: actual timestamp not stored in basic Message
            )
        }

        return ChatHistoryResponse(
            sessionId = sessionId,
            messages = messageDtos
        )
    }

    /**
     * Closes a session.
     */
    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close a session")
    suspend fun closeSession(
        @PathVariable sessionId: String,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        val session = sessionService.getSession(sessionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")

        // Verify ownership
        val adherentId = extractAdherentId(jwt)
        if (session.adherentId != adherentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        sessionService.closeSession(sessionId)
        logger.info { "Closed session: $sessionId" }
    }

    /**
     * Gets session statistics.
     */
    @GetMapping("/{sessionId}/stats")
    @Operation(summary = "Get session statistics")
    suspend fun getSessionStats(
        @PathVariable sessionId: String,
        @AuthenticationPrincipal jwt: Jwt
    ): Map<String, Any> {
        val stats = sessionService.getSessionStats(sessionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")

        // Verify ownership
        val adherentId = extractAdherentId(jwt)
        if (stats.adherentId != adherentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        return mapOf(
            "sessionId" to stats.sessionId,
            "status" to stats.status.name,
            "messageCount" to stats.messageCount,
            "durationSeconds" to stats.durationSeconds,
            "isEscalated" to stats.isEscalated
        )
    }

    private fun extractAdherentId(jwt: Jwt): UUID {
        val adherentIdClaim = jwt.getClaimAsString("adherent_id")
            ?: jwt.getClaimAsString("sub")
            ?: throw IllegalStateException("No adherent_id found in JWT")

        return try {
            UUID.fromString(adherentIdClaim)
        } catch (e: IllegalArgumentException) {
            UUID.nameUUIDFromBytes(adherentIdClaim.toByteArray())
        }
    }
}
