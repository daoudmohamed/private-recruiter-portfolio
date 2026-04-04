package com.knowledgebase.api.controller

import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.application.service.SessionService
import com.knowledgebase.domain.model.ConversationSession
import com.knowledgebase.domain.model.SessionStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

class SessionControllerTest {

    private val sessionService: SessionService = mockk()
    private val chatMemory: RedisChatMemory = mockk()
    private val controller = SessionController(sessionService, chatMemory)

    @Test
    fun `createSession should return session dto`() = runBlocking {
        val session = ConversationSession(
            id = "session-123",
            startedAt = Instant.parse("2026-03-26T10:00:00Z"),
            lastActivityAt = Instant.parse("2026-03-26T10:05:00Z"),
            status = SessionStatus.ACTIVE,
            messageCount = 2
        )
        coEvery { sessionService.createSession() } returns session

        val response = controller.createSession(com.knowledgebase.application.dto.CreateSessionRequest())

        assertThat(response.id).isEqualTo("session-123")
        assertThat(response.status).isEqualTo(SessionStatus.ACTIVE)
        assertThat(response.messageCount).isEqualTo(2)
    }

    @Test
    fun `getSession should throw not found when session is missing`() {
        coEvery { sessionService.getSession("missing") } returns null

        val thrown = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            runBlocking { controller.getSession("missing") }
        }

        assertThat(thrown.statusCode.value()).isEqualTo(404)
    }

    @Test
    fun `getHistory should map redis chat messages to dto response`() = runBlocking {
        coEvery { chatMemory.get("session-123", 50) } returns mutableListOf(
            UserMessage("Bonjour"),
            AssistantMessage("Salut")
        )

        val response = controller.getHistory("session-123", 50)

        assertThat(response.sessionId).isEqualTo("session-123")
        assertThat(response.messages).hasSize(2)
        assertThat(response.messages[0].role).isEqualTo("USER")
        assertThat(response.messages[0].content).isEqualTo("Bonjour")
        assertThat(response.messages[1].role).isEqualTo("ASSISTANT")
        assertThat(response.messages[1].content).isEqualTo("Salut")
    }
}
