package com.knowledgebase.config

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "knowledgebase")
data class KnowledgeBaseProperties(
    @field:Valid
    val security: Security = Security(),
    @field:Valid
    val recruiterAccess: RecruiterAccess = RecruiterAccess(),
    @field:Valid
    val chat: Chat = Chat(),
    @field:Valid
    val rag: Rag = Rag(),
    @field:Valid
    val documents: Documents = Documents(),
    @field:Valid
    val qdrant: Qdrant = Qdrant()
) {
    data class Security(
        val apiKey: String = "",
        val adminApiKey: String = "",
        @field:NotEmpty
        val publicPaths: List<@NotBlank String> = listOf(
            "/",
            "/index.html",
            "/vite.svg",
            "/favicon.ico",
            "/assets/**",
            "/access",
            "/access/**",
            "/actuator/health/**",
            "/actuator/info",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/api/v1/recruiter-access/request-invitation",
            "/api/v1/recruiter-access/consume",
            "/api/v1/recruiter-access/session",
            "/api/v1/recruiter-access/logout"
        ),
        @field:NotEmpty
        val adminPaths: List<@NotBlank String> = listOf(
            "/api/v1/recruiter-access/invitations"
        ),
        @field:Valid
        val cors: Cors = Cors()
    ) {
        data class Cors(
            @field:NotEmpty
            val allowedOrigins: List<@NotBlank String> = listOf(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:4173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174",
                "http://127.0.0.1:4173"
            ),
            @field:NotEmpty
            val allowedMethods: List<@NotBlank String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
            @field:NotEmpty
            val allowedHeaders: List<@NotBlank String> = listOf("*"),
            val allowCredentials: Boolean = true,
            @field:Positive
            val maxAgeSeconds: Long = 3600
        )
    }

    data class RecruiterAccess(
        val enabled: Boolean = false,
        val requestInvitationEnabled: Boolean = false,
        @field:Positive
        @field:Max(168)
        val invitationTtlHours: Long = 24,
        @field:Positive
        @field:Max(168)
        val sessionTtlHours: Long = 24,
        val cookieName: String = "kb_recruiter_session",
        val secureCookies: Boolean = false,
        val frontendBaseUrl: String = "",
        val tokenSecret: String = "",
        @field:Valid
        val captcha: Captcha = Captcha(),
        @field:Valid
        val rateLimit: RateLimit = RateLimit(),
        @field:Valid
        val email: Email = Email()
    ) {
        data class Captcha(
            val provider: Provider = Provider.NONE,
            val siteKey: String = "",
            val verifyEnabled: Boolean = true,
            @field:Valid
            val recaptcha: Recaptcha = Recaptcha()
        ) {
            data class Recaptcha(
                val secretKey: String = "",
                val verifyUrl: String = "https://www.google.com/recaptcha/api/siteverify",
                @field:jakarta.validation.constraints.DecimalMin("0.0")
                @field:jakarta.validation.constraints.DecimalMax("1.0")
                val minimumScore: Double = 0.5,
                val action: String = "request_invitation"
            )

            enum class Provider {
                NONE,
                RECAPTCHA_V3
            }
        }

        data class RateLimit(
            @field:Positive
            @field:Max(1000)
            val maxRequestsPerIp: Int = 5,
            @field:Positive
            @field:Max(1000)
            val maxRequestsPerEmail: Int = 3,
            @field:Positive
            @field:Max(1440)
            val windowMinutes: Long = 60
        )

        data class Email(
            val provider: Provider = Provider.LOG,
            val fromEmail: String = "",
            val fromName: String = "Knowledge Base",
            val subject: String = "Votre acces recruteur",
            @field:Valid
            val brevo: Brevo = Brevo()
        ) {
            data class Brevo(
                val apiKey: String = "",
                val templateId: Long? = null,
                val replyTo: String = ""
            )

            enum class Provider {
                LOG,
                BREVO
            }
        }
    }

    data class Chat(
        @field:Positive
        val maxHistoryMessages: Int = 20,
        @field:Max(168)
        @field:Positive
        val sessionTimeoutHours: Long = 24
    )

    data class Rag(
        @field:DecimalMin("0.0")
        @field:DecimalMax("1.0")
        val similarityThreshold: Double = 0.65,
        @field:Max(20)
        @field:Positive
        val topK: Int = 5
    )

    data class Documents(
        @field:NotBlank
        val folder: String = "documents/",
        val scanOnStartup: Boolean = true
    )

    data class Qdrant(
        val configureOnStartup: Boolean = true
    )
}
