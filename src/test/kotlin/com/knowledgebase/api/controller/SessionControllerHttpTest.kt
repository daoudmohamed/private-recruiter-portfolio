package com.knowledgebase.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.application.service.SessionService
import com.knowledgebase.domain.model.ConversationSession
import com.knowledgebase.domain.model.SessionStatus
import com.knowledgebase.support.fetchCsrfToken
import io.mockk.coEvery
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SessionControllerHttpTest {

    @LocalServerPort
    private var port: Int = 0

    @MockkBean
    private lateinit var sessionService: SessionService

    @MockkBean
    private lateinit var chatMemory: RedisChatMemory

    @MockkBean
    private lateinit var vectorStore: VectorStore

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        assertNotNull(port)
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `post sessions should create a session`() {
        val csrfToken = webTestClient.fetchCsrfToken()

        coEvery { sessionService.createSession() } returns ConversationSession(
            id = "session-123",
            startedAt = Instant.parse("2026-03-26T10:00:00Z"),
            lastActivityAt = Instant.parse("2026-03-26T10:00:00Z"),
            status = SessionStatus.ACTIVE,
            messageCount = 0
        )

        webTestClient.post()
            .uri("/api/v1/sessions")
            .cookie("XSRF-TOKEN", csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{}""")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isEqualTo("session-123")
            .jsonPath("$.status").isEqualTo("ACTIVE")
            .jsonPath("$.messageCount").isEqualTo(0)
    }

    @Test
    fun `get session should return not found payload when missing`() {
        coEvery { sessionService.getSession("missing-session") } returns null

        webTestClient.get()
            .uri("/api/v1/sessions/missing-session")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.message").isEqualTo("Session not found")
            .jsonPath("$.path").isEqualTo("/api/v1/sessions/missing-session")
    }
}
