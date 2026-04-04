package com.knowledgebase.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant
import java.util.UUID

/**
 * Represents a conversation session.
 */
data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Instant = Instant.now(),
    val lastActivityAt: Instant = Instant.now(),
    val status: SessionStatus = SessionStatus.ACTIVE,
    val messageCount: Int = 0,
    val metadata: Map<String, Any> = emptyMap()
) {
    @get:JsonIgnore
    val isActive: Boolean
        get() = status == SessionStatus.ACTIVE

    @get:JsonIgnore
    val durationSeconds: Long
        get() = java.time.Duration.between(startedAt, lastActivityAt).seconds

    fun withUpdatedActivity(): ConversationSession = copy(
        lastActivityAt = Instant.now(),
        messageCount = messageCount + 1
    )
}

/**
 * Status of a conversation session.
 */
enum class SessionStatus {
    ACTIVE,
    IDLE,
    CLOSED
}

/**
 * A message in a conversation.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant = Instant.now(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Role of the message sender.
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Chunk of a streaming chat response.
 */
data class ChatChunk(
    val type: ChunkType,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun content(text: String) = ChatChunk(ChunkType.CONTENT, text)
        fun error(message: String) = ChatChunk(ChunkType.ERROR, message)
        fun done() = ChatChunk(ChunkType.DONE, "")
    }
}

/**
 * Type of chat chunk.
 */
enum class ChunkType {
    CONTENT,
    ERROR,
    DONE
}

/**
 * Request to send a chat message.
 */
data class ChatRequest(
    val sessionId: String,
    val message: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Response to a chat message (non-streaming).
 */
data class ChatResponse(
    val sessionId: String,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)
