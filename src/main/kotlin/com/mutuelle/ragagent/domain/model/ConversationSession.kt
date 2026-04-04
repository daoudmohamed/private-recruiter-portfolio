package com.mutuelle.ragagent.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mutuelle.ragagent.domain.intent.Intent
import java.time.Instant
import java.util.UUID

/**
 * Represents a conversation session with an adherent.
 */
data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    val adherentId: UUID,
    val startedAt: Instant = Instant.now(),
    val lastActivityAt: Instant = Instant.now(),
    val status: SessionStatus = SessionStatus.ACTIVE,
    val currentIntent: Intent? = null,
    val messageCount: Int = 0,
    val escalated: Boolean = false,
    val escalationReason: String? = null,
    val escalatedAt: Instant? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    @get:JsonIgnore
    val isActive: Boolean
        get() = status == SessionStatus.ACTIVE

    @get:JsonIgnore
    val isEscalated: Boolean
        get() = status == SessionStatus.ESCALATED || escalated

    @get:JsonIgnore
    val durationSeconds: Long
        get() = java.time.Duration.between(startedAt, lastActivityAt).seconds

    fun withUpdatedActivity(intent: Intent? = null): ConversationSession = copy(
        lastActivityAt = Instant.now(),
        currentIntent = intent ?: currentIntent,
        messageCount = messageCount + 1
    )

    fun withEscalation(reason: String): ConversationSession = copy(
        status = SessionStatus.ESCALATED,
        escalated = true,
        escalationReason = reason,
        escalatedAt = Instant.now(),
        lastActivityAt = Instant.now()
    )
}

/**
 * Status of a conversation session.
 */
enum class SessionStatus {
    ACTIVE,
    IDLE,
    ESCALATED,
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
        fun escalation(message: String) = ChatChunk(ChunkType.ESCALATION, message)
        fun error(message: String) = ChatChunk(ChunkType.ERROR, message)
        fun done() = ChatChunk(ChunkType.DONE, "")
    }
}

/**
 * Type of chat chunk.
 */
enum class ChunkType {
    CONTENT,
    ESCALATION,
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
    val intent: Intent? = null,
    val escalated: Boolean = false,
    val metadata: Map<String, Any> = emptyMap()
)
