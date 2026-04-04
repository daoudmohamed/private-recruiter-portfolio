package com.mutuelle.ragagent.domain.event

import com.mutuelle.ragagent.domain.intent.Intent
import java.time.Instant
import java.util.UUID

/**
 * Base interface for domain events.
 */
sealed interface DomainEvent {
    val eventId: String
    val timestamp: Instant
    val adherentId: UUID?
}

/**
 * Event emitted when a chat message is received.
 */
data class ChatMessageReceivedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val adherentId: UUID,
    val sessionId: String,
    val message: String,
    val detectedIntent: Intent?
) : DomainEvent

/**
 * Event emitted when a chat response is sent.
 */
data class ChatResponseSentEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val adherentId: UUID,
    val sessionId: String,
    val responseLength: Int,
    val processingTimeMs: Long,
    val tokensUsed: Int?
) : DomainEvent

/**
 * Event emitted when a conversation is escalated to a human agent.
 */
data class EscalationEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val adherentId: UUID,
    val sessionId: String,
    val reason: String,
    val lastMessage: String,
    val conversationSummary: String? = null,
    val priority: EscalationPriority = EscalationPriority.NORMAL,
    val metadata: Map<String, Any> = emptyMap()
) : DomainEvent

/**
 * Priority level for escalation.
 */
enum class EscalationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Event emitted when an escalation is resolved.
 */
data class EscalationResolvedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val adherentId: UUID,
    val sessionId: String,
    val resolution: String,
    val resolvedBy: String,
    val resolutionTimeMinutes: Long
) : DomainEvent

/**
 * Event emitted when a document is requested.
 */
data class DocumentRequestedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val adherentId: UUID,
    val documentType: String,
    val requestedVia: String = "chat"
) : DomainEvent

/**
 * Event emitted when a document is generated.
 */
data class DocumentGeneratedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val adherentId: UUID,
    val documentId: UUID,
    val documentType: String,
    val generationTimeMs: Long
) : DomainEvent

/**
 * Event emitted for analytics purposes.
 */
data class ConversationAnalyticsEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    override val adherentId: UUID?,
    val sessionId: String,
    val intentDetected: Intent?,
    val intentCorrect: Boolean? = null,
    val resolvedFirstMessage: Boolean,
    val escalationTriggered: Boolean,
    val escalationReason: String? = null,
    val satisfaction: Int? = null,
    val durationSeconds: Long,
    val messageCount: Int
) : DomainEvent
