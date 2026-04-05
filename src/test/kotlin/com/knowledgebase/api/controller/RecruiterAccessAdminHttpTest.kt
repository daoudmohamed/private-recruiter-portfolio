package com.knowledgebase.api.controller

import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.application.service.CaptchaVerificationService
import com.knowledgebase.application.service.InvitationRequestRateLimitService
import com.knowledgebase.application.service.RecruiterAccessSessionService
import com.knowledgebase.application.service.RecruiterInvitationService
import com.knowledgebase.domain.model.RecruiterInvitation
import com.knowledgebase.domain.model.RecruiterInvitationStatus
import com.knowledgebase.support.fetchCsrfToken
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "knowledgebase.security.admin-api-key=test-admin-key",
        "knowledgebase.recruiter-access.enabled=true",
        "knowledgebase.recruiter-access.frontend-base-url=http://localhost:5173",
        "knowledgebase.recruiter-access.token-secret=test-token-secret"
    ]
)
@ActiveProfiles("test")
class RecruiterAccessAdminHttpTest {

    @LocalServerPort
    private var port: Int = 0

    @MockkBean
    private lateinit var recruiterInvitationService: RecruiterInvitationService

    @MockkBean
    private lateinit var recruiterAccessSessionService: RecruiterAccessSessionService

    @MockkBean
    private lateinit var captchaVerificationService: CaptchaVerificationService

    @MockkBean
    private lateinit var invitationRequestRateLimitService: InvitationRequestRateLimitService

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
    fun `admin invitation creation should reject valid api key without csrf token`() {
        webTestClient.post()
            .uri("/api/v1/recruiter-access/invitations")
            .header("X-API-Key", "test-admin-key")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email":"recruiter@example.com"}""")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `admin invitation creation should allow valid api key with csrf token`() {
        coEvery { recruiterInvitationService.createInvitation("recruiter@example.com", null) } returns RecruiterInvitation(
            id = "invitation-123",
            email = "recruiter@example.com",
            status = RecruiterInvitationStatus.PENDING,
            createdAt = Instant.parse("2026-04-04T20:00:00Z"),
            expiresAt = Instant.parse("2026-04-05T20:00:00Z")
        )
        val csrfToken = webTestClient.fetchCsrfToken()

        webTestClient.post()
            .uri("/api/v1/recruiter-access/invitations")
            .header("X-API-Key", "test-admin-key")
            .cookie("XSRF-TOKEN", csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email":"recruiter@example.com"}""")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.invitationId").isEqualTo("invitation-123")
            .jsonPath("$.email").isEqualTo("recruiter@example.com")
            .jsonPath("$.status").isEqualTo("PENDING")
    }
}
