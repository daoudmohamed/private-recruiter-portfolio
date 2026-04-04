package com.mutuelle.ragagent.api.controller

import com.mutuelle.ragagent.application.service.ChatService
import com.mutuelle.ragagent.application.service.SessionService
import com.mutuelle.ragagent.domain.model.ChatChunk
import com.mutuelle.ragagent.domain.model.ChatRequest
import com.mutuelle.ragagent.domain.model.ChatResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * REST controller for chat operations.
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "Chat and conversation endpoints")
class ChatController(
    private val chatService: ChatService,
    private val sessionService: SessionService
) {
    /**
     * Streams a chat response via Server-Sent Events.
     */
    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(summary = "Send a chat message and receive streaming response")
    suspend fun streamChat(
        @RequestBody request: ChatRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): Flow<ServerSentEvent<ChatChunk>> {
        val adherentId = extractAdherentId(jwt)
        logger.info { "Streaming chat for adherent: $adherentId, session: ${request.sessionId}" }

        return chatService.streamChat(request, adherentId)
            .map { chunk ->
                ServerSentEvent.builder(chunk)
                    .event(chunk.type.name.lowercase())
                    .build()
            }
    }

    /**
     * Sends a chat message and receives a synchronous response.
     */
    @PostMapping("/sync")
    @Operation(summary = "Send a chat message and receive synchronous response")
    suspend fun chat(
        @RequestBody request: ChatRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ChatResponse {
        val adherentId = extractAdherentId(jwt)
        logger.info { "Sync chat for adherent: $adherentId, session: ${request.sessionId}" }

        return chatService.chat(request, adherentId)
    }

    /**
     * Validates that a session exists and is active.
     */
    @GetMapping("/validate/{sessionId}")
    @Operation(summary = "Validate if a session is active")
    suspend fun validateSession(
        @PathVariable sessionId: String,
        @AuthenticationPrincipal jwt: Jwt
    ): Map<String, Any> {
        val isActive = sessionService.isSessionActive(sessionId)
        return mapOf(
            "sessionId" to sessionId,
            "isActive" to isActive
        )
    }

    private fun extractAdherentId(jwt: Jwt): UUID {
        val adherentIdClaim = jwt.getClaimAsString("adherent_id")
            ?: jwt.getClaimAsString("sub")
            ?: throw IllegalStateException("No adherent_id found in JWT")

        return try {
            UUID.fromString(adherentIdClaim)
        } catch (e: IllegalArgumentException) {
            // If it's not a UUID, generate a deterministic one from the string
            UUID.nameUUIDFromBytes(adherentIdClaim.toByteArray())
        }
    }
}
