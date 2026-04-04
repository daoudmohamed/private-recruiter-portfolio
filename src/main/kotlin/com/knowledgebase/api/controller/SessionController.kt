package com.knowledgebase.api.controller

import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.application.dto.ChatHistoryResponse
import com.knowledgebase.application.dto.ChatMessageDto
import com.knowledgebase.application.dto.CreateSessionRequest
import com.knowledgebase.application.dto.SessionResponse
import com.knowledgebase.application.dto.toDto
import com.knowledgebase.application.service.SessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

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
    @Operation(summary = "Create a new conversation session")
    suspend fun createSession(
        @RequestBody request: CreateSessionRequest
    ): SessionResponse {
        logger.info { "Creating new session" }
        val session = sessionService.createSession()
        return session.toDto()
    }

    /**
     * Gets session details by ID.
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session details")
    suspend fun getSession(
        @PathVariable sessionId: String
    ): SessionResponse {
        val session = sessionService.getSession(sessionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")

        return session.toDto()
    }

    /**
     * Gets chat history for a session.
     */
    @GetMapping("/{sessionId}/history")
    @Operation(summary = "Get chat history for a session")
    suspend fun getHistory(
        @PathVariable sessionId: String,
        @RequestParam(defaultValue = "50") limit: Int
    ): ChatHistoryResponse {
        val messages = chatMemory.get(sessionId, limit)
        val messageDtos = messages.map { message ->
            ChatMessageDto(
                role = message.messageType.name,
                content = message.text,
                timestamp = java.time.Instant.now() // Redis doesn't store timestamps by default
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
    @Operation(summary = "Close a session")
    suspend fun closeSession(
        @PathVariable sessionId: String
    ): Map<String, String> {
        sessionService.closeSession(sessionId)
        logger.info { "Closed session: $sessionId" }

        return mapOf("status" to "Session closed")
    }
}
