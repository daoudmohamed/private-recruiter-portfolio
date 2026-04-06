package com.knowledgebase.api.controller

import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.application.service.CaptchaVerificationService
import com.knowledgebase.application.service.InvitationRequestRateLimitService
import com.knowledgebase.application.service.RecruiterAccessSessionService
import com.knowledgebase.application.service.RecruiterInvitationService
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "knowledgebase.recruiter-access.enabled=true",
        "knowledgebase.recruiter-access.request-invitation-enabled=true",
        "knowledgebase.recruiter-access.frontend-base-url=http://localhost:5173",
        "knowledgebase.recruiter-access.captcha.provider=RECAPTCHA_V3",
        "knowledgebase.recruiter-access.captcha.site-key=",
        "knowledgebase.recruiter-access.captcha.recaptcha.action="
    ]
)
@ActiveProfiles("test")
class RecruiterAccessControllerBlankCaptchaHttpTest {

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
    fun `session should omit blank captcha metadata`() {
        webTestClient.get()
            .uri("/api/v1/recruiter-access/session")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.captchaSiteKey").isEmpty()
            .jsonPath("$.captchaAction").isEmpty()
    }
}
