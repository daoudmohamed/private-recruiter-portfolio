package com.knowledgebase.api.controller

import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.application.service.CaptchaVerificationService
import com.knowledgebase.application.service.InvitationRequestRateLimitService
import com.knowledgebase.application.service.RecruiterAccessException
import com.knowledgebase.application.service.RecruiterAccessSessionService
import com.knowledgebase.application.service.RecruiterInvitationService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coJustRun
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["knowledgebase.recruiter-access.frontend-base-url=http://localhost:5173"]
)
@ActiveProfiles("test")
class RecruiterAccessControllerHttpTest {

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
    fun `consume should expose stable recruiter access error code`() {
        coEvery { recruiterInvitationService.consumeInvitation("used-token") } throws RecruiterAccessException(
            status = HttpStatus.BAD_REQUEST,
            reason = "Lien d'acces deja utilise",
            code = "recruiter_access.link_already_used"
        )

        webTestClient.post()
            .uri("/api/v1/recruiter-access/consume")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"token":"used-token"}""")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.message").isEqualTo("Lien d'acces deja utilise")
            .jsonPath("$.code").isEqualTo("recruiter_access.link_already_used")
            .jsonPath("$.path").isEqualTo("/api/v1/recruiter-access/consume")
    }

    @Test
    fun `logout should reject cross-origin request`() {
        webTestClient.post()
            .uri("/api/v1/recruiter-access/logout")
            .header("Origin", "https://evil.example")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `logout should allow configured frontend origin`() {
        coJustRun { recruiterAccessSessionService.invalidateSession("session-123") }
        coEvery { recruiterAccessSessionService.clearSessionCookie() } returns ResponseCookie.from("kb_recruiter_session", "")
            .path("/")
            .maxAge(0)
            .build()

        webTestClient.post()
            .uri("/api/v1/recruiter-access/logout")
            .header("Origin", "http://localhost:5173")
            .cookie("kb_recruiter_session", "session-123")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").value<Boolean> { assertTrue(it) }
    }
}
