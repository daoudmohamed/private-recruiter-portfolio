package com.knowledgebase.api.controller

import com.knowledgebase.application.dto.ConsumeRecruiterAccessRequest
import com.knowledgebase.application.dto.CreateRecruiterInvitationRequest
import com.knowledgebase.application.dto.RequestRecruiterInvitationRequest
import com.knowledgebase.application.dto.RequestRecruiterInvitationResponse
import com.knowledgebase.application.dto.RecruiterAccessSessionResponse
import com.knowledgebase.application.dto.RecruiterInvitationResponse
import com.knowledgebase.application.service.CaptchaVerificationService
import com.knowledgebase.application.service.InvitationRequestRateLimitService
import com.knowledgebase.application.service.RecruiterAccessSessionService
import com.knowledgebase.application.service.RecruiterInvitationService
import com.knowledgebase.config.KnowledgeBaseProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import java.net.URI

@RestController
@RequestMapping("/api/v1/recruiter-access")
@Tag(name = "Recruiter Access", description = "Temporary recruiter access flow")
class RecruiterAccessController(
    private val knowledgeBaseProperties: KnowledgeBaseProperties,
    private val recruiterInvitationService: RecruiterInvitationService,
    private val recruiterAccessSessionService: RecruiterAccessSessionService,
    private val captchaVerificationService: CaptchaVerificationService,
    private val invitationRequestRateLimitService: InvitationRequestRateLimitService
) {
    @PostMapping("/request-invitation")
    @Operation(summary = "Request a recruiter invitation by email")
    suspend fun requestInvitation(
        @RequestBody request: RequestRecruiterInvitationRequest,
        exchange: ServerWebExchange
    ): RequestRecruiterInvitationResponse {
        val remoteIp = exchange.request.headers.getFirst("X-Forwarded-For")
            ?.substringBefore(',')
            ?.trim()
            ?: exchange.request.remoteAddress?.address?.hostAddress

        val result = recruiterInvitationService.requestInvitation(
            email = request.email,
            remoteIp = remoteIp,
            captchaToken = request.captchaToken,
            captchaVerificationService = captchaVerificationService,
            rateLimitService = invitationRequestRateLimitService
        )

        return RequestRecruiterInvitationResponse(
            accepted = result.accepted,
            message = result.message
        )
    }

    @PostMapping("/invitations")
    @Operation(summary = "Create and send a recruiter invitation")
    suspend fun createInvitation(
        @RequestBody request: CreateRecruiterInvitationRequest
    ): RecruiterInvitationResponse {
        val invitation = recruiterInvitationService.createInvitation(request.email, request.expiresInHours)
        return RecruiterInvitationResponse(
            invitationId = invitation.id,
            email = invitation.email,
            expiresAt = invitation.expiresAt,
            status = invitation.status.name
        )
    }

    @PostMapping("/consume")
    @Operation(summary = "Consume a recruiter access link")
    suspend fun consume(
        @RequestBody request: ConsumeRecruiterAccessRequest,
        exchange: ServerWebExchange
    ): RecruiterAccessSessionResponse {
        val session = recruiterInvitationService.consumeInvitation(request.token)
        exchange.response.addCookie(recruiterAccessSessionService.createSessionCookie(session))
        return RecruiterAccessSessionResponse(
            enabled = knowledgeBaseProperties.recruiterAccess.enabled,
            authenticated = true,
            requestInvitationEnabled = knowledgeBaseProperties.recruiterAccess.requestInvitationEnabled,
            captchaSiteKey = knowledgeBaseProperties.recruiterAccess.captcha.siteKey.ifBlank { null },
            captchaAction = knowledgeBaseProperties.recruiterAccess.captcha.recaptcha.action.ifBlank { null },
            expiresAt = session.expiresAt
        )
    }

    @GetMapping("/session")
    @Operation(summary = "Get current recruiter access session")
    suspend fun session(
        exchange: ServerWebExchange
    ): RecruiterAccessSessionResponse {
        if (!knowledgeBaseProperties.recruiterAccess.enabled) {
            return RecruiterAccessSessionResponse(
                enabled = false,
                authenticated = true,
                requestInvitationEnabled = false
            )
        }

        val cookie = exchange.request.cookies.getFirst(knowledgeBaseProperties.recruiterAccess.cookieName)
        val session = cookie?.value?.let { recruiterAccessSessionService.getSession(it) }
        return RecruiterAccessSessionResponse(
            enabled = true,
            authenticated = session != null,
            requestInvitationEnabled = knowledgeBaseProperties.recruiterAccess.requestInvitationEnabled,
            captchaSiteKey = knowledgeBaseProperties.recruiterAccess.captcha.siteKey.ifBlank { null },
            captchaAction = knowledgeBaseProperties.recruiterAccess.captcha.recaptcha.action.ifBlank { null },
            expiresAt = session?.expiresAt
        )
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current recruiter session")
    suspend fun logout(
        exchange: ServerWebExchange
    ): Map<String, Boolean> {
        validateSameOriginLogoutRequest(exchange)

        val cookie = exchange.request.cookies.getFirst(knowledgeBaseProperties.recruiterAccess.cookieName)
        if (cookie != null) {
            recruiterAccessSessionService.invalidateSession(cookie.value)
        }
        exchange.response.addCookie(recruiterAccessSessionService.clearSessionCookie())
        return mapOf("success" to true)
    }

    private fun validateSameOriginLogoutRequest(exchange: ServerWebExchange) {
        val configuredBaseUrl = knowledgeBaseProperties.recruiterAccess.frontendBaseUrl.trim()
        if (configuredBaseUrl.isBlank()) {
            return
        }

        val expectedOrigin = runCatching {
            val uri = URI(configuredBaseUrl)
            "${uri.scheme}://${uri.authority}"
        }.getOrNull() ?: return

        val requestOrigin = exchange.request.headers.getFirst(HttpHeaders.ORIGIN)?.trim()
        if (requestOrigin == null || requestOrigin != expectedOrigin) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid request origin")
        }
    }
}
