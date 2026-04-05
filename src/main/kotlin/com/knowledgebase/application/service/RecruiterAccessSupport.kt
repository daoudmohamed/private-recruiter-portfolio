package com.knowledgebase.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.knowledgebase.config.KnowledgeBaseProperties
import com.knowledgebase.config.RedisKeyspace
import com.knowledgebase.domain.model.RecruiterAccessSession
import com.knowledgebase.domain.model.RecruiterInvitation
import com.knowledgebase.domain.model.RecruiterInvitationStatus
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters

private val logger = KotlinLogging.logger {}
private const val GENERIC_INVITATION_MESSAGE = "Si cette adresse est eligibile, un lien d'acces a ete envoye."

interface InvitationEmailSender {
    suspend fun sendInvitation(email: String, magicLink: String, expiresAt: Instant)
}

interface CaptchaVerificationService {
    suspend fun verify(captchaToken: String?, remoteIp: String?): Boolean
}

class RecruiterAccessException(
    status: HttpStatus,
    reason: String,
    val code: String
) : ResponseStatusException(status, reason)

@Service
class RecruiterInvitationTokenService(
    private val knowledgeBaseProperties: KnowledgeBaseProperties
) {
    private val secureRandom = SecureRandom()

    fun createToken(invitationId: String): GeneratedInvitationToken {
        val secret = ByteArray(32).also { secureRandom.nextBytes(it) }
        val secretEncoded = base64UrlEncode(secret)
        val token = "$invitationId.$secretEncoded"
        return GeneratedInvitationToken(
            rawToken = token,
            tokenHash = sign(token)
        )
    }

    fun parse(token: String): ParsedInvitationToken {
        val parts = token.split('.')
        if (parts.size != 2) {
            throw RecruiterAccessException(HttpStatus.BAD_REQUEST, "Lien d'acces invalide", "recruiter_access.invalid_link")
        }

        return ParsedInvitationToken(
            invitationId = parts[0],
            tokenHash = sign(token)
        )
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secret = knowledgeBaseProperties.recruiterAccess.tokenSecret
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return base64UrlEncode(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

data class GeneratedInvitationToken(
    val rawToken: String,
    val tokenHash: String
)

data class ParsedInvitationToken(
    val invitationId: String,
    val tokenHash: String
)

@Service
class RecruiterAccessSessionService(
    @param:Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val knowledgeBaseProperties: KnowledgeBaseProperties
) {
    suspend fun createSession(email: String, invitationId: String): RecruiterAccessSession {
        val expiresAt = Instant.now().plus(Duration.ofHours(knowledgeBaseProperties.recruiterAccess.sessionTtlHours))
        val session = RecruiterAccessSession(
            email = email,
            invitationId = invitationId,
            expiresAt = expiresAt
        )
        val key = RedisKeyspace.RECRUITER_SESSION_PREFIX + session.id
        redisTemplate.opsForValue()
            .set(key, objectMapper.writeValueAsString(session), Duration.between(Instant.now(), expiresAt))
            .awaitSingle()
        return session
    }

    fun createSessionCookie(session: RecruiterAccessSession): ResponseCookie {
        val ttl = Duration.between(Instant.now(), session.expiresAt).coerceAtLeast(Duration.ZERO)
        return ResponseCookie.from(knowledgeBaseProperties.recruiterAccess.cookieName, session.id)
            .httpOnly(true)
            .secure(knowledgeBaseProperties.recruiterAccess.secureCookies)
            .sameSite("Lax")
            .path("/")
            .maxAge(ttl)
            .build()
    }

    fun clearSessionCookie(): ResponseCookie =
        ResponseCookie.from(knowledgeBaseProperties.recruiterAccess.cookieName, "")
            .httpOnly(true)
            .secure(knowledgeBaseProperties.recruiterAccess.secureCookies)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ZERO)
            .build()

    suspend fun getSession(sessionId: String): RecruiterAccessSession? {
        val key = RedisKeyspace.RECRUITER_SESSION_PREFIX + sessionId
        val raw = redisTemplate.opsForValue().get(key).awaitSingleOrNull() ?: return null
        val session = objectMapper.readValue<RecruiterAccessSession>(raw)
        if (session.expiresAt.isBefore(Instant.now())) {
            invalidateSession(sessionId)
            return null
        }
        return session
    }

    suspend fun invalidateSession(sessionId: String) {
        val key = RedisKeyspace.RECRUITER_SESSION_PREFIX + sessionId
        redisTemplate.delete(key).awaitSingleOrNull()
    }

    suspend fun resolveSession(exchange: ServerWebExchange): RecruiterAccessSession? {
        val cookie = exchange.request.cookies.getFirst(knowledgeBaseProperties.recruiterAccess.cookieName)
            ?: return null
        return getSession(cookie.value)
    }
}

@Service
class RecruiterInvitationService(
    @param:Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val knowledgeBaseProperties: KnowledgeBaseProperties,
    private val tokenService: RecruiterInvitationTokenService,
    private val recruiterAccessSessionService: RecruiterAccessSessionService,
    private val invitationEmailSender: InvitationEmailSender
) {
    suspend fun createInvitation(email: String, expiresInHours: Long? = null): RecruiterInvitation {
        val normalizedEmail = email.trim().lowercase()
        validateEmail(normalizedEmail)

        val invitation = findReusableInvitation(normalizedEmail, expiresInHours)
            ?: newInvitation(normalizedEmail, expiresInHours)

        val invitationWithFreshToken = sendInvitationEmail(invitation)
        logger.info { "Recruiter invitation created or refreshed for ${normalizedEmail.maskEmail()} with id ${invitation.id}" }
        return invitationWithFreshToken
    }

    suspend fun requestInvitation(
        email: String,
        remoteIp: String?,
        captchaToken: String?,
        captchaVerificationService: CaptchaVerificationService,
        rateLimitService: InvitationRequestRateLimitService
    ): InvitationRequestResult {
        val normalizedEmail = email.trim().lowercase()
        validateEmail(normalizedEmail)

        if (!knowledgeBaseProperties.recruiterAccess.requestInvitationEnabled) {
            logger.info { "Public recruiter invitation request ignored because feature is disabled" }
            return InvitationRequestResult(accepted = true, message = GENERIC_INVITATION_MESSAGE)
        }

        val ipAllowed = rateLimitService.tryAcquireForIp(remoteIp)
        val emailAllowed = rateLimitService.tryAcquireForEmail(normalizedEmail)
        val captchaValid = captchaVerificationService.verify(captchaToken, remoteIp)

        if (!ipAllowed || !emailAllowed || !captchaValid) {
            logger.warn {
                "Recruiter invitation request ignored for ${normalizedEmail.maskEmail()} (ipAllowed=$ipAllowed, emailAllowed=$emailAllowed, captchaValid=$captchaValid)"
            }
            return InvitationRequestResult(accepted = true, message = GENERIC_INVITATION_MESSAGE)
        }

        createInvitation(normalizedEmail)
        return InvitationRequestResult(accepted = true, message = GENERIC_INVITATION_MESSAGE)
    }

    suspend fun consumeInvitation(token: String): RecruiterAccessSession {
        val parsed = tokenService.parse(token)
        val invitation = getInvitation(parsed.invitationId)
            ?: throw RecruiterAccessException(HttpStatus.BAD_REQUEST, "Lien d'acces invalide", "recruiter_access.invalid_link")

        if (invitation.tokenHash != parsed.tokenHash) {
            throw RecruiterAccessException(HttpStatus.BAD_REQUEST, "Lien d'acces invalide", "recruiter_access.invalid_link")
        }
        if (invitation.status != RecruiterInvitationStatus.PENDING || invitation.consumedAt != null) {
            throw RecruiterAccessException(HttpStatus.BAD_REQUEST, "Lien d'acces deja utilise", "recruiter_access.link_already_used")
        }
        if (invitation.expiresAt.isBefore(Instant.now())) {
            save(invitation.copy(status = RecruiterInvitationStatus.EXPIRED))
            throw RecruiterAccessException(HttpStatus.BAD_REQUEST, "Lien d'acces expire", "recruiter_access.link_expired")
        }

        val consumed = invitation.copy(
            consumedAt = Instant.now(),
            status = RecruiterInvitationStatus.CONSUMED
        )
        save(consumed)
        logger.info { "Recruiter invitation consumed for ${invitation.email.maskEmail()} with id ${invitation.id}" }
        return recruiterAccessSessionService.createSession(invitation.email, invitation.id)
    }

    private suspend fun findReusableInvitation(email: String, expiresInHours: Long?): RecruiterInvitation? {
        val invitationId = redisTemplate.opsForValue()
            .get(RedisKeyspace.RECRUITER_INVITATION_EMAIL_PREFIX + email)
            .awaitSingleOrNull()
            ?: return null

        val invitation = getInvitation(invitationId) ?: return null
        return when {
            invitation.status == RecruiterInvitationStatus.PENDING && invitation.expiresAt.isAfter(Instant.now()) ->
                invitation.copy(
                    expiresAt = Instant.now().plus(Duration.ofHours(expiresInHours ?: knowledgeBaseProperties.recruiterAccess.invitationTtlHours))
                )
            invitation.expiresAt.isBefore(Instant.now()) && invitation.status == RecruiterInvitationStatus.PENDING -> {
                save(invitation.copy(status = RecruiterInvitationStatus.EXPIRED))
                null
            }
            else -> null
        }
    }

    private suspend fun getInvitation(invitationId: String): RecruiterInvitation? {
        val key = RedisKeyspace.RECRUITER_INVITATION_PREFIX + invitationId
        val raw = redisTemplate.opsForValue().get(key).awaitSingleOrNull() ?: return null
        return objectMapper.readValue(raw)
    }

    private suspend fun save(invitation: RecruiterInvitation) {
        val key = RedisKeyspace.RECRUITER_INVITATION_PREFIX + invitation.id
        val ttl = Duration.between(Instant.now(), invitation.expiresAt.plus(Duration.ofHours(24))).coerceAtLeast(Duration.ofHours(1))
        redisTemplate.opsForValue()
            .set(key, objectMapper.writeValueAsString(invitation), ttl)
            .awaitSingle()
        redisTemplate.opsForValue()
            .set(RedisKeyspace.RECRUITER_INVITATION_EMAIL_PREFIX + invitation.email, invitation.id, ttl)
            .awaitSingle()
    }

    private fun newInvitation(email: String, expiresInHours: Long?): RecruiterInvitation {
        val ttlHours = expiresInHours ?: knowledgeBaseProperties.recruiterAccess.invitationTtlHours
        val expiresAt = Instant.now().plus(Duration.ofHours(ttlHours))
        val invitationId = UUID.randomUUID().toString()
        return RecruiterInvitation(
            id = invitationId,
            email = email,
            tokenHash = "",
            expiresAt = expiresAt
        )
    }

    private suspend fun sendInvitationEmail(invitation: RecruiterInvitation): RecruiterInvitation {
        val generatedToken = tokenService.createToken(invitation.id)
        val refreshedInvitation = invitation.copy(tokenHash = generatedToken.tokenHash)
        save(refreshedInvitation)
        val magicLink = buildMagicLink(generatedToken.rawToken)
        invitationEmailSender.sendInvitation(refreshedInvitation.email, magicLink, refreshedInvitation.expiresAt)
        return refreshedInvitation
    }

    private fun buildMagicLink(token: String): String {
        val baseUrl = knowledgeBaseProperties.recruiterAccess.frontendBaseUrl.trimEnd('/')
        return "$baseUrl/access?token=${urlEncode(token)}"
    }

    private fun urlEncode(token: String): String =
        java.net.URLEncoder.encode(token, StandardCharsets.UTF_8)

    private fun validateEmail(email: String) {
        if (!EMAIL_REGEX.matches(email)) {
            throw RecruiterAccessException(HttpStatus.BAD_REQUEST, "Adresse email invalide", "recruiter_access.invalid_email")
        }
    }
}

@Service
class TransactionalInvitationEmailSender(
    private val knowledgeBaseProperties: KnowledgeBaseProperties,
    webClientBuilder: WebClient.Builder
) : InvitationEmailSender {
    private val webClient = webClientBuilder
        .baseUrl("https://api.brevo.com/v3")
        .build()

    override suspend fun sendInvitation(email: String, magicLink: String, expiresAt: Instant) {
        val config = knowledgeBaseProperties.recruiterAccess.email
        when (config.provider) {
            KnowledgeBaseProperties.RecruiterAccess.Email.Provider.LOG -> {
                logger.info { "Recruiter invitation email for ${email.maskEmail()}: $magicLink (expires at $expiresAt)" }
            }

            KnowledgeBaseProperties.RecruiterAccess.Email.Provider.BREVO -> {
                val brevoConfig = config.brevo
                val basePayload = mapOf(
                    "sender" to mapOf(
                        "name" to config.fromName,
                        "email" to config.fromEmail
                    ),
                    "to" to listOf(mapOf("email" to email)),
                    "subject" to config.subject,
                    "templateId" to brevoConfig.templateId,
                    "params" to mapOf(
                        "magicLink" to magicLink,
                        "expiresAtLabel" to formatExpiryLabel(expiresAt)
                    )
                )
                val payload =
                    if (brevoConfig.replyTo.isBlank()) {
                        basePayload
                    } else {
                        basePayload + ("replyTo" to mapOf("email" to brevoConfig.replyTo))
                    }

                webClient.post()
                    .uri("/smtp/email")
                    .header("api-key", brevoConfig.apiKey)
                    .header("accept", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .awaitSingle()

                logger.info { "Recruiter invitation email sent to ${email.maskEmail()}" }
            }
        }
    }

    private fun formatExpiryLabel(expiresAt: Instant): String =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm")
            .withZone(java.time.ZoneId.of("Europe/Paris"))
            .format(expiresAt)
}

@Service
class InvitationRequestRateLimitService(
    @param:Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val knowledgeBaseProperties: KnowledgeBaseProperties,
    private val environment: Environment
) {
    suspend fun tryAcquireForIp(remoteIp: String?): Boolean {
        if (isDevProfileActive()) {
            return true
        }
        val ip = remoteIp?.trim()?.ifBlank { "unknown" } ?: "unknown"
        return tryAcquire(
            RedisKeyspace.RECRUITER_RATE_LIMIT_IP_PREFIX + ip,
            knowledgeBaseProperties.recruiterAccess.rateLimit.maxRequestsPerIp
        )
    }

    suspend fun tryAcquireForEmail(email: String): Boolean {
        if (isDevProfileActive()) {
            return true
        }
        return tryAcquire(
            RedisKeyspace.RECRUITER_RATE_LIMIT_EMAIL_PREFIX + email,
            knowledgeBaseProperties.recruiterAccess.rateLimit.maxRequestsPerEmail
        )
    }

    private fun isDevProfileActive(): Boolean =
        environment.acceptsProfiles(Profiles.of("dev"))

    private suspend fun tryAcquire(key: String, maxRequests: Int): Boolean {
        val count = redisTemplate.opsForValue().increment(key).awaitSingle()
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(knowledgeBaseProperties.recruiterAccess.rateLimit.windowMinutes))
                .awaitSingleOrNull()
        }
        return count <= maxRequests
    }
}

@Service
class GoogleCaptchaVerificationService(
    private val knowledgeBaseProperties: KnowledgeBaseProperties,
    webClientBuilder: WebClient.Builder
) : CaptchaVerificationService {
    private val webClient = webClientBuilder.build()

    override suspend fun verify(captchaToken: String?, remoteIp: String?): Boolean {
        val captchaConfig = knowledgeBaseProperties.recruiterAccess.captcha
        if (!captchaConfig.verifyEnabled || captchaConfig.provider == KnowledgeBaseProperties.RecruiterAccess.Captcha.Provider.NONE) {
            return true
        }

        if (captchaToken.isNullOrBlank()) {
            return false
        }

        return when (captchaConfig.provider) {
            KnowledgeBaseProperties.RecruiterAccess.Captcha.Provider.NONE -> true
            KnowledgeBaseProperties.RecruiterAccess.Captcha.Provider.RECAPTCHA_V3 -> verifyRecaptcha(captchaToken, remoteIp)
        }
    }

    private suspend fun verifyRecaptcha(captchaToken: String, remoteIp: String?): Boolean {
        val captchaConfig = knowledgeBaseProperties.recruiterAccess.captcha
        val response = webClient.post()
            .uri(captchaConfig.recaptcha.verifyUrl)
            .body(
                BodyInserters.fromFormData("secret", captchaConfig.recaptcha.secretKey)
                    .with("response", captchaToken)
                    .apply {
                        if (!remoteIp.isNullOrBlank()) {
                            with("remoteip", remoteIp)
                        }
                    }
            )
            .retrieve()
            .bodyToMono(RecaptchaVerificationResponse::class.java)
            .awaitSingle()

        return response.isAccepted(captchaConfig.recaptcha)
    }
}

data class InvitationRequestResult(
    val accepted: Boolean,
    val message: String
)

internal data class RecaptchaVerificationResponse(
    val success: Boolean = false,
    val score: Double? = null,
    val action: String? = null
)

internal fun RecaptchaVerificationResponse.isAccepted(config: KnowledgeBaseProperties.RecruiterAccess.Captcha.Recaptcha): Boolean {
    if (!success) {
        return false
    }

    val responseAction = action?.trim()
    if (responseAction.isNullOrEmpty() || responseAction != config.action) {
        return false
    }

    val responseScore = score ?: return false
    return responseScore >= config.minimumScore
}

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun String.maskEmail(): String {
    val parts = split("@")
    if (parts.size != 2) return "***"
    val localPart = parts[0]
    val maskedLocalPart = when {
        localPart.length <= 2 -> "*".repeat(localPart.length)
        else -> localPart.take(2) + "*".repeat(localPart.length - 2)
    }
    return "$maskedLocalPart@${parts[1]}"
}

private fun Duration.coerceAtLeast(min: Duration): Duration =
    if (this < min) min else this
