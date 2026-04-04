package com.knowledgebase.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.knowledgebase.application.service.ChatService
import com.knowledgebase.application.service.SessionService
import com.knowledgebase.domain.model.ChatChunk
import com.knowledgebase.domain.model.ChatRequest
import com.knowledgebase.domain.model.ChatResponse
import com.knowledgebase.support.fetchCsrfToken
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatControllerHttpTest {

    @LocalServerPort
    private var port: Int = 0

    @MockkBean
    private lateinit var chatService: ChatService

    @MockkBean
    private lateinit var sessionService: SessionService

    @MockkBean
    private lateinit var vectorStore: VectorStore

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `post chat sync should return aggregated response`() {
        val csrfToken = webTestClient.fetchCsrfToken()
        val request = ChatRequest(sessionId = "session-123", message = "Bonjour")

        coEvery { chatService.chat(request) } returns ChatResponse(
            sessionId = "session-123",
            content = "Salut Mohamed"
        )

        webTestClient.post()
            .uri("/api/v1/chat/sync")
            .cookie("XSRF-TOKEN", csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "sessionId": "session-123",
                  "message": "Bonjour"
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.sessionId").isEqualTo("session-123")
            .jsonPath("$.content").isEqualTo("Salut Mohamed")
    }

    @Test
    fun `post chat should stream server sent events`() {
        val csrfToken = webTestClient.fetchCsrfToken()
        val request = ChatRequest(sessionId = "session-123", message = "Bonjour")

        every { chatService.streamChat(request) } returns flowOf(
            ChatChunk.content("Salut"),
            ChatChunk.done()
        )

        val body = webTestClient.post()
            .uri("/api/v1/chat")
            .cookie("XSRF-TOKEN", csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(
                """
                {
                  "sessionId": "session-123",
                  "message": "Bonjour"
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .returnResult(String::class.java)
            .responseBody
            .take(2)
            .collectList()
            .block()
            .orEmpty()
            .joinToString("\n")

        org.assertj.core.api.Assertions.assertThat(body).contains("\"type\":\"CONTENT\"")
        org.assertj.core.api.Assertions.assertThat(body).contains("\"content\":\"Salut\"")
        org.assertj.core.api.Assertions.assertThat(body).contains("\"type\":\"DONE\"")
    }
}
