package com.knowledgebase.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.config.KnowledgeBaseProperties
import com.knowledgebase.domain.model.ConversationSession
import com.knowledgebase.domain.model.SessionStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

class SessionServiceTest {

    private lateinit var redisTemplate: ReactiveRedisTemplate<String, String>
    private lateinit var valueOperations: ReactiveValueOperations<String, String>
    private lateinit var chatMemory: RedisChatMemory
    private lateinit var objectMapper: ObjectMapper
    private lateinit var service: SessionService

    @BeforeEach
    fun setUp() {
        redisTemplate = mockk()
        valueOperations = mockk()
        chatMemory = mockk()
        objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())

        every { redisTemplate.opsForValue() } returns valueOperations
        every { valueOperations.set(any(), any(), any<Duration>()) } returns Mono.just(true)
        every { redisTemplate.delete(any<String>()) } returns Mono.just(1L)
        coEvery { chatMemory.clearAsync(any()) } just runs
        coEvery { chatMemory.getMessageCount(any()) } returns 0L

        service = SessionService(
            redisTemplate = redisTemplate,
            objectMapper = objectMapper,
            chatMemory = chatMemory,
            knowledgeBaseProperties = KnowledgeBaseProperties(
                chat = KnowledgeBaseProperties.Chat(
                    maxHistoryMessages = 20,
                    sessionTimeoutHours = 24
                )
            )
        )
    }

    @Test
    fun `createSession should persist a new active session with configured ttl`() = runBlocking {
        val jsonSlot = slot<String>()

        every {
            valueOperations.set(match { it.startsWith("session:") }, capture(jsonSlot), Duration.ofHours(24))
        } returns Mono.just(true)

        val session = service.createSession()

        assertThat(session.id).isNotBlank()
        assertThat(session.status).isEqualTo(SessionStatus.ACTIVE)

        val persisted = objectMapper.readValue(jsonSlot.captured, ConversationSession::class.java)
        assertThat(persisted.id).isEqualTo(session.id)
        assertThat(persisted.status).isEqualTo(SessionStatus.ACTIVE)
    }

    @Test
    fun `getSession should deserialize stored session`() = runBlocking {
        val session = ConversationSession(
            id = "session-123",
            startedAt = Instant.parse("2026-03-26T10:00:00Z"),
            lastActivityAt = Instant.parse("2026-03-26T10:05:00Z"),
            status = SessionStatus.ACTIVE,
            messageCount = 2
        )

        every { valueOperations.get("session:session-123") } returns Mono.just(
            objectMapper.writeValueAsString(session)
        )

        val result = service.getSession("session-123")

        assertThat(result).isEqualTo(session)
    }

    @Test
    fun `updateActivity should increment message count and refresh last activity`() = runBlocking {
        val session = ConversationSession(
            id = "session-123",
            startedAt = Instant.parse("2026-03-26T10:00:00Z"),
            lastActivityAt = Instant.parse("2026-03-26T10:05:00Z"),
            status = SessionStatus.ACTIVE,
            messageCount = 2
        )
        val jsonSlot = slot<String>()

        every { valueOperations.get("session:session-123") } returns Mono.just(
            objectMapper.writeValueAsString(session)
        )
        every {
            valueOperations.set("session:session-123", capture(jsonSlot), Duration.ofHours(24))
        } returns Mono.just(true)

        service.updateActivity("session-123")

        val persisted = objectMapper.readValue(jsonSlot.captured, ConversationSession::class.java)
        assertThat(persisted.messageCount).isEqualTo(3)
        assertThat(persisted.lastActivityAt).isAfter(session.lastActivityAt)
    }

    @Test
    fun `closeSession should mark session closed and clear chat memory`() = runBlocking {
        val session = ConversationSession(
            id = "session-123",
            startedAt = Instant.parse("2026-03-26T10:00:00Z"),
            lastActivityAt = Instant.parse("2026-03-26T10:05:00Z"),
            status = SessionStatus.ACTIVE
        )
        val jsonSlot = slot<String>()

        every { valueOperations.get("session:session-123") } returns Mono.just(
            objectMapper.writeValueAsString(session)
        )
        every {
            valueOperations.set("session:session-123", capture(jsonSlot), Duration.ofHours(24))
        } returns Mono.just(true)

        service.closeSession("session-123")

        val persisted = objectMapper.readValue(jsonSlot.captured, ConversationSession::class.java)
        assertThat(persisted.status).isEqualTo(SessionStatus.CLOSED)
        coVerify(exactly = 1) { chatMemory.clearAsync("session-123") }
    }
}
