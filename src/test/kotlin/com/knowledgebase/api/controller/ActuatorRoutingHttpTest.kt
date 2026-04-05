package com.knowledgebase.api.controller

import com.knowledgebase.ai.memory.RedisChatMemory
import com.ninjasquad.springmockk.MockkBean
import org.springframework.ai.vectorstore.VectorStore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ActuatorRoutingHttpTest {

    @LocalServerPort
    private var port: Int = 0

    @MockkBean
    private lateinit var chatMemory: RedisChatMemory

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
    fun `actuator health should not be served by spa fallback`() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectBody(String::class.java)
            .value { body ->
                check(!body.orEmpty().contains("<!doctype html>", ignoreCase = true)) {
                    "Actuator health should not be served by SPA fallback. Body was: $body"
                }
            }
    }
}
