package com.knowledgebase.config

import org.springframework.context.SmartLifecycle
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Component

@Component
class SecurityStartupValidator(
    private val knowledgeBaseProperties: KnowledgeBaseProperties,
    private val environment: Environment
) : SmartLifecycle {

    private var running = false

    override fun start() {
        if (!isRelaxedProfileActive()) {
            validateSecurity()
            validateRecruiterAccess()
        }
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean = running

    override fun isAutoStartup(): Boolean = true

    override fun getPhase(): Int = Int.MIN_VALUE

    private fun isRelaxedProfileActive(): Boolean =
        environment.acceptsProfiles(Profiles.of("dev", "test"))

    private fun validateSecurity() {
        if (effectiveAdminApiKey().isBlank()) {
            throw IllegalStateException(
                "knowledgebase.security.admin-api-key or knowledgebase.security.api-key must be configured outside dev/test profiles"
            )
        }
    }

    private fun validateRecruiterAccess() {
        val recruiterAccess = knowledgeBaseProperties.recruiterAccess
        if (!recruiterAccess.enabled) {
            return
        }

        requireNotBlank(recruiterAccess.tokenSecret, "knowledgebase.recruiter-access.token-secret")
        requireNotBlank(recruiterAccess.frontendBaseUrl, "knowledgebase.recruiter-access.frontend-base-url")

        if (recruiterAccess.email.provider == KnowledgeBaseProperties.RecruiterAccess.Email.Provider.BREVO) {
            requireNotBlank(recruiterAccess.email.fromEmail, "knowledgebase.recruiter-access.email.from-email")
            requireNotBlank(recruiterAccess.email.brevo.apiKey, "knowledgebase.recruiter-access.email.brevo.api-key")
            if (recruiterAccess.email.brevo.templateId == null || recruiterAccess.email.brevo.templateId <= 0) {
                throw IllegalStateException("knowledgebase.recruiter-access.email.brevo.template-id must be configured when recruiter access email provider is BREVO")
            }
        }

        if (recruiterAccess.requestInvitationEnabled &&
            recruiterAccess.captcha.verifyEnabled &&
            recruiterAccess.captcha.provider == KnowledgeBaseProperties.RecruiterAccess.Captcha.Provider.RECAPTCHA_V3
        ) {
            requireNotBlank(recruiterAccess.captcha.siteKey, "knowledgebase.recruiter-access.captcha.site-key")
            requireNotBlank(recruiterAccess.captcha.recaptcha.secretKey, "knowledgebase.recruiter-access.captcha.recaptcha.secret-key")
            requireNotBlank(recruiterAccess.captcha.recaptcha.action, "knowledgebase.recruiter-access.captcha.recaptcha.action")
        }
    }

    private fun effectiveAdminApiKey(): String =
        knowledgeBaseProperties.security.adminApiKey.ifBlank {
            knowledgeBaseProperties.security.apiKey
        }

    private fun requireNotBlank(value: String, propertyName: String) {
        if (value.isBlank()) {
            throw IllegalStateException("$propertyName must be configured when recruiter access is enabled")
        }
    }
}
