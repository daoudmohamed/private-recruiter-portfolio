package com.knowledgebase.api.controller

import com.knowledgebase.application.service.ChatService
import com.knowledgebase.application.service.SessionService
import com.knowledgebase.domain.model.ChatChunk
import com.knowledgebase.domain.model.ChatRequest
import com.knowledgebase.domain.model.ChatResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*

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
        @RequestBody request: ChatRequest
    ): Flow<ServerSentEvent<ChatChunk>> {
        logger.info { "Streaming chat for session: ${request.sessionId}" }

        return chatService.streamChat(request)
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
        @RequestBody request: ChatRequest
    ): ChatResponse {
        logger.info { "Sync chat for session: ${request.sessionId}" }

        return chatService.chat(request)
    }
}
