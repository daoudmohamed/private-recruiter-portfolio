package com.knowledgebase.api.controller

import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.application.service.CaptchaVerificationService
import com.knowledgebase.application.service.InvitationRequestRateLimitService
import com.knowledgebase.application.service.RecruiterAccessException
import com.knowledgebase.application.service.RecruiterAccessSessionService
import com.knowledgebase.application.service.RecruiterInvitationService
import com.knowledgebase.support.fetchCsrfToken
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
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "knowledgebase.recruiter-access.enabled=true",
        "knowledgebase.recruiter-access.request-invitation-enabled=true",
        "knowledgebase.recruiter-access.frontend-base-url=http://localhost:5173",
        "knowledgebase.recruiter-access.captcha.provider=RECAPTCHA_V3",
        "knowledgebase.recruiter-access.captcha.site-key=test-site-key",
        "knowledgebase.recruiter-access.captcha.recaptcha.action=request_invitation"
    ]
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
        val csrfToken = webTestClient.fetchCsrfToken()

        webTestClient.post()
            .uri("/api/v1/recruiter-access/logout")
            .cookie("XSRF-TOKEN", csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .header("Origin", "https://evil.example")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `logout should allow configured frontend origin`() {
        val csrfToken = webTestClient.fetchCsrfToken()

        coJustRun { recruiterAccessSessionService.invalidateSession("session-123") }
        coEvery { recruiterAccessSessionService.clearSessionCookie() } returns ResponseCookie.from("kb_recruiter_session", "")
            .path("/")
            .maxAge(0)
            .build()

        webTestClient.post()
            .uri("/api/v1/recruiter-access/logout")
            .cookie("XSRF-TOKEN", csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .header("Origin", "http://localhost:5173")
            .cookie("kb_recruiter_session", "session-123")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").value<Boolean> { assertTrue(it) }
    }

    @Test
    fun `session should expose recaptcha v3 action metadata`() {
        webTestClient.get()
            .uri("/api/v1/recruiter-access/session")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.captchaSiteKey").isEqualTo("test-site-key")
            .jsonPath("$.captchaAction").isEqualTo("request_invitation")
    }

    @Test
    fun `consume should expose recaptcha v3 action metadata`() {
        coEvery { recruiterInvitationService.consumeInvitation("valid-token") } returns com.knowledgebase.domain.model.RecruiterAccessSession(
            id = "session-1",
            email = "recruteur@example.com",
            invitationId = "invite-1",
            expiresAt = Instant.parse("2026-04-06T12:00:00Z")
        )
        coEvery { recruiterAccessSessionService.createSessionCookie(any()) } returns ResponseCookie.from("kb_recruiter_session", "session-1")
            .httpOnly(true)
            .path("/")
            .build()

        webTestClient.post()
            .uri("/api/v1/recruiter-access/consume")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"token":"valid-token"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authenticated").isEqualTo(true)
            .jsonPath("$.captchaSiteKey").isEqualTo("test-site-key")
            .jsonPath("$.captchaAction").isEqualTo("request_invitation")
    }
}
