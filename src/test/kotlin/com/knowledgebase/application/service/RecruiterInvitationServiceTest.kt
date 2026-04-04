package com.knowledgebase.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.knowledgebase.config.KnowledgeBaseProperties
import com.knowledgebase.config.RedisKeyspace
import com.knowledgebase.domain.model.RecruiterInvitation
import com.knowledgebase.domain.model.RecruiterInvitationStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

class RecruiterInvitationServiceTest {

    private lateinit var redisTemplate: ReactiveRedisTemplate<String, String>
    private lateinit var valueOperations: ReactiveValueOperations<String, String>
    private lateinit var objectMapper: ObjectMapper
    private lateinit var tokenService: RecruiterInvitationTokenService
    private lateinit var sessionService: RecruiterAccessSessionService
    private lateinit var emailSender: InvitationEmailSender
    private lateinit var captchaService: CaptchaVerificationService
    private lateinit var rateLimitService: InvitationRequestRateLimitService
    private lateinit var service: RecruiterInvitationService

    @BeforeEach
    fun setUp() {
        redisTemplate = mockk()
        valueOperations = mockk()
        objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
        sessionService = mockk(relaxed = true)
        emailSender = mockk()
        captchaService = mockk()
        rateLimitService = mockk()

        every { redisTemplate.opsForValue() } returns valueOperations
        every { valueOperations.get(any()) } returns Mono.empty()
        every {
            valueOperations.set(any<String>(), any<String>(), any<Duration>())
        } returns Mono.just(true)
        coEvery { emailSender.sendInvitation(any(), any(), any()) } just runs

        val properties = KnowledgeBaseProperties(
            recruiterAccess = KnowledgeBaseProperties.RecruiterAccess(
                enabled = true,
                requestInvitationEnabled = true,
                tokenSecret = "secret",
                frontendBaseUrl = "https://portfolio.example.com"
            )
        )
        tokenService = RecruiterInvitationTokenService(properties)

        service = RecruiterInvitationService(
            redisTemplate = redisTemplate,
            objectMapper = objectMapper,
            knowledgeBaseProperties = properties,
            tokenService = tokenService,
            recruiterAccessSessionService = sessionService,
            invitationEmailSender = emailSender
        )
    }

    @Test
    fun `requestInvitation should return generic response when captcha is invalid`() = runBlocking {
        coEvery { rateLimitService.tryAcquireForIp(any()) } returns true
        coEvery { rateLimitService.tryAcquireForEmail(any()) } returns true
        coEvery { captchaService.verify(any(), any()) } returns false

        val result = service.requestInvitation(
            email = "recruteur@example.com",
            remoteIp = "127.0.0.1",
            captchaToken = "bad-token",
            captchaVerificationService = captchaService,
            rateLimitService = rateLimitService
        )

        assertThat(result.accepted).isTrue()
        assertThat(result.message).contains("lien d'acces")
        coVerify(exactly = 0) { emailSender.sendInvitation(any(), any(), any()) }
    }

    @Test
    fun `requestInvitation should return generic response when rate limited`() = runBlocking {
        coEvery { rateLimitService.tryAcquireForIp(any()) } returns false
        coEvery { rateLimitService.tryAcquireForEmail(any()) } returns true
        coEvery { captchaService.verify(any(), any()) } returns true

        val result = service.requestInvitation(
            email = "recruteur@example.com",
            remoteIp = "127.0.0.1",
            captchaToken = "valid-token",
            captchaVerificationService = captchaService,
            rateLimitService = rateLimitService
        )

        assertThat(result.accepted).isTrue()
        assertThat(result.message).contains("lien d'acces")
        coVerify(exactly = 0) { emailSender.sendInvitation(any(), any(), any()) }
    }

    @Test
    fun `requestInvitation should create and send invitation when checks pass`() = runBlocking {
        coEvery { rateLimitService.tryAcquireForIp(any()) } returns true
        coEvery { rateLimitService.tryAcquireForEmail(any()) } returns true
        coEvery { captchaService.verify(any(), any()) } returns true

        val result = service.requestInvitation(
            email = "recruteur@example.com",
            remoteIp = "127.0.0.1",
            captchaToken = "valid-token",
            captchaVerificationService = captchaService,
            rateLimitService = rateLimitService
        )

        assertThat(result.accepted).isTrue()
        assertThat(result.message).contains("lien d'acces")
        coVerify(exactly = 1) { emailSender.sendInvitation("recruteur@example.com", any(), any()) }
    }

    @Test
    fun `createInvitation should reuse active invitation for same email`() = runBlocking {
        val invitation = service.createInvitation("recruteur@example.com")
        val invitationJson = objectMapper.writeValueAsString(invitation)

        every {
            valueOperations.get(RedisKeyspace.RECRUITER_INVITATION_EMAIL_PREFIX + "recruteur@example.com")
        } returns Mono.just(invitation.id)
        every {
            valueOperations.get(RedisKeyspace.RECRUITER_INVITATION_PREFIX + invitation.id)
        } returns Mono.just(invitationJson)

        val reused = service.createInvitation("recruteur@example.com")

        assertThat(reused.id).isEqualTo(invitation.id)
        coVerify(exactly = 2) { emailSender.sendInvitation("recruteur@example.com", any(), any()) }
    }

    @Test
    fun `createInvitation should reject invalid email`() {
        assertThatThrownBy {
            runBlocking {
                service.createInvitation("invalid-email")
            }
        }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `consumeInvitation should expose stable code when link is already used`() = runBlocking {
        val generatedToken = tokenService.createToken("invite-1")
        val invitation = RecruiterInvitation(
            id = "invite-1",
            email = "recruteur@example.com",
            tokenHash = generatedToken.tokenHash,
            expiresAt = Instant.now().plusSeconds(3600),
            consumedAt = Instant.now(),
            status = RecruiterInvitationStatus.CONSUMED
        )

        every {
            valueOperations.get(RedisKeyspace.RECRUITER_INVITATION_PREFIX + "invite-1")
        } returns Mono.just(objectMapper.writeValueAsString(invitation))

        assertThatThrownBy {
            runBlocking {
                service.consumeInvitation(generatedToken.rawToken)
            }
        }
            .isInstanceOf(RecruiterAccessException::class.java)
            .extracting { (it as RecruiterAccessException).code }
            .isEqualTo("recruiter_access.link_already_used")
    }
}
