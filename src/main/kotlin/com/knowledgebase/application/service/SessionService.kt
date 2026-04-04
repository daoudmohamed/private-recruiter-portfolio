package com.knowledgebase.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.domain.model.ConversationSession
import com.knowledgebase.domain.model.SessionStatus
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Service for managing conversation sessions.
 */
@Service
class SessionService(
    @Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val chatMemory: RedisChatMemory,
    @Value("\${knowledgebase.chat.session-timeout-hours:24}")
    private val sessionTimeoutHours: Long
) {
    private val sessionPrefix = "session:"
    private val sessionTtl = Duration.ofHours(sessionTimeoutHours)

    /**
     * Creates a new conversation session.
     */
    suspend fun createSession(): ConversationSession {
        val session = ConversationSession(
            id = UUID.randomUUID().toString(),
            startedAt = Instant.now(),
            lastActivityAt = Instant.now(),
            status = SessionStatus.ACTIVE
        )

        saveSession(session)
        logger.info { "Created session: ${session.id}" }

        return session
    }

    /**
     * Gets a session by ID.
     */
    suspend fun getSession(sessionId: String): ConversationSession? {
        val key = "$sessionPrefix$sessionId"
        return try {
            redisTemplate.opsForValue()
                .get(key)
                .awaitSingleOrNull()
                ?.let { objectMapper.readValue<ConversationSession>(it) }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get session: $sessionId" }
            null
        }
    }

    /**
     * Updates session activity timestamp.
     */
    suspend fun updateActivity(sessionId: String) {
        val session = getSession(sessionId) ?: return

        val updated = session.withUpdatedActivity()
        saveSession(updated)

        logger.debug { "Updated activity for session: $sessionId" }
    }

    /**
     * Updates a session with new data.
     */
    suspend fun updateSession(session: ConversationSession) {
        saveSession(session.copy(lastActivityAt = Instant.now()))
    }

    /**
     * Closes a session.
     */
    suspend fun closeSession(sessionId: String) {
        val session = getSession(sessionId) ?: return

        val closed = session.copy(
            status = SessionStatus.CLOSED,
            lastActivityAt = Instant.now()
        )
        saveSession(closed)

        // Clear chat memory
        chatMemory.clearAsync(sessionId)

        logger.info { "Closed session: $sessionId" }
    }

    /**
     * Checks if a session exists and is active.
     */
    suspend fun isSessionActive(sessionId: String): Boolean {
        val session = getSession(sessionId)
        return session?.isActive == true
    }

    /**
     * Gets session statistics.
     */
    suspend fun getSessionStats(sessionId: String): SessionStats? {
        val session = getSession(sessionId) ?: return null
        val messageCount = chatMemory.getMessageCount(sessionId)

        return SessionStats(
            sessionId = sessionId,
            status = session.status,
            messageCount = messageCount.toInt(),
            durationSeconds = session.durationSeconds
        )
    }

    private suspend fun saveSession(session: ConversationSession) {
        val key = "$sessionPrefix${session.id}"
        try {
            redisTemplate.opsForValue()
                .set(key, objectMapper.writeValueAsString(session), sessionTtl)
                .awaitSingle()
        } catch (e: Exception) {
            logger.error(e) { "Failed to save session: ${session.id}" }
        }
    }
}

/**
 * Statistics for a session.
 */
data class SessionStats(
    val sessionId: String,
    val status: SessionStatus,
    val messageCount: Int,
    val durationSeconds: Long
)
