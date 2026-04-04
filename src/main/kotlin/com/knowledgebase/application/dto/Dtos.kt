package com.knowledgebase.application.dto

import com.knowledgebase.domain.model.*
import java.time.Instant

/**
 * DTO for creating a new session.
 */
data class CreateSessionRequest(
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * DTO for session response.
 */
data class SessionResponse(
    val id: String,
    val status: SessionStatus,
    val startedAt: Instant,
    val lastActivityAt: Instant,
    val messageCount: Int = 0
)

/**
 * DTO for chat history response.
 */
data class ChatHistoryResponse(
    val sessionId: String,
    val messages: List<ChatMessageDto>
)

data class ChatMessageDto(
    val role: String,
    val content: String,
    val timestamp: Instant
)

/**
 * DTO for document upload response.
 */
data class DocumentUploadResponse(
    val success: Boolean,
    val filename: String,
    val message: String,
    val chunksCreated: Int = 0
)

/**
 * DTO for ingestion status response.
 */
data class IngestionStatusResponse(
    val totalDocuments: Int,
    val documentsIngested: List<String>,
    val status: String
)

// Extension functions
fun ConversationSession.toDto() = SessionResponse(
    id = id,
    status = status,
    startedAt = startedAt,
    lastActivityAt = lastActivityAt,
    messageCount = messageCount
)
