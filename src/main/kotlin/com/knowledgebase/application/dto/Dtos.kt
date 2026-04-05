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

data class CreateRecruiterInvitationRequest(
    val email: String,
    val expiresInHours: Long? = null
)

data class RequestRecruiterInvitationRequest(
    val email: String,
    val captchaToken: String? = null
)

data class RequestRecruiterInvitationResponse(
    val accepted: Boolean,
    val message: String
)

data class RecruiterInvitationResponse(
    val invitationId: String,
    val email: String,
    val expiresAt: Instant,
    val status: String
)

data class ConsumeRecruiterAccessRequest(
    val token: String
)

data class RecruiterAccessSessionResponse(
    val enabled: Boolean,
    val authenticated: Boolean,
    val requestInvitationEnabled: Boolean = false,
    val captchaSiteKey: String? = null,
    val captchaAction: String? = null,
    val expiresAt: Instant? = null
)

// Extension functions
fun ConversationSession.toDto() = SessionResponse(
    id = id,
    status = status,
    startedAt = startedAt,
    lastActivityAt = lastActivityAt,
    messageCount = messageCount
)
