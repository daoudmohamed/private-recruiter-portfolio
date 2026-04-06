package com.knowledgebase.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

class SecurityStartupValidatorTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration::class.java)

    @Test
    fun `should fail startup without admin api key outside dev and test`() {
        contextRunner
            .run { context ->
                assertThat(context.startupFailure).isNotNull
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalStateException::class.java)
                    .hasStackTraceContaining("knowledgebase.security.admin-api-key")
            }
    }

    @Test
    fun `should allow startup without admin api key in dev profile`() {
        contextRunner
            .withSystemProperties("spring.profiles.active=dev")
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context).hasSingleBean(SecurityStartupValidator::class.java)
            }
    }

    @Test
    fun `should allow startup when admin api key is configured`() {
        contextRunner
            .withPropertyValues("knowledgebase.security.admin-api-key=secret-key")
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context).hasSingleBean(SecurityStartupValidator::class.java)
            }
    }

    @Test
    fun `should fail when recruiter access is enabled without required secrets`() {
        contextRunner
            .withPropertyValues(
                "knowledgebase.security.admin-api-key=secret-key",
                "knowledgebase.recruiter-access.enabled=true"
            )
            .run { context ->
                assertThat(context.startupFailure).isNotNull
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalStateException::class.java)
                    .hasStackTraceContaining("knowledgebase.recruiter-access.token-secret")
            }
    }

    @Test
    fun `should fail when public invitation requests require recaptcha without secret`() {
        contextRunner
            .withPropertyValues(
                "knowledgebase.security.admin-api-key=secret-key",
                "knowledgebase.recruiter-access.enabled=true",
                "knowledgebase.recruiter-access.token-secret=super-secret",
                "knowledgebase.recruiter-access.frontend-base-url=https://portfolio.example.com",
                "knowledgebase.recruiter-access.request-invitation-enabled=true",
                "knowledgebase.recruiter-access.captcha.provider=RECAPTCHA_V3",
                "knowledgebase.recruiter-access.captcha.verify-enabled=true"
            )
            .run { context ->
                assertThat(context.startupFailure).isNotNull
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalStateException::class.java)
                    .hasStackTraceContaining("knowledgebase.recruiter-access.captcha.site-key")
            }
    }

    @Test
    fun `should fail when public invitation requests require recaptcha without action`() {
        contextRunner
            .withPropertyValues(
                "knowledgebase.security.admin-api-key=secret-key",
                "knowledgebase.recruiter-access.enabled=true",
                "knowledgebase.recruiter-access.token-secret=super-secret",
                "knowledgebase.recruiter-access.frontend-base-url=https://portfolio.example.com",
                "knowledgebase.recruiter-access.request-invitation-enabled=true",
                "knowledgebase.recruiter-access.captcha.provider=RECAPTCHA_V3",
                "knowledgebase.recruiter-access.captcha.site-key=site-key",
                "knowledgebase.recruiter-access.captcha.verify-enabled=true",
                "knowledgebase.recruiter-access.captcha.recaptcha.secret-key=captcha-secret",
                "knowledgebase.recruiter-access.captcha.recaptcha.action="
            )
            .run { context ->
                assertThat(context.startupFailure).isNotNull
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalStateException::class.java)
                    .hasStackTraceContaining("knowledgebase.recruiter-access.captcha.recaptcha.action")
            }
    }

    @Test
    fun `should allow startup with complete recaptcha v3 configuration`() {
        contextRunner
            .withPropertyValues(
                "knowledgebase.security.admin-api-key=secret-key",
                "knowledgebase.recruiter-access.enabled=true",
                "knowledgebase.recruiter-access.token-secret=super-secret",
                "knowledgebase.recruiter-access.frontend-base-url=https://portfolio.example.com",
                "knowledgebase.recruiter-access.request-invitation-enabled=true",
                "knowledgebase.recruiter-access.captcha.provider=RECAPTCHA_V3",
                "knowledgebase.recruiter-access.captcha.site-key=site-key",
                "knowledgebase.recruiter-access.captcha.verify-enabled=true",
                "knowledgebase.recruiter-access.captcha.recaptcha.secret-key=captcha-secret",
                "knowledgebase.recruiter-access.captcha.recaptcha.action=request_invitation"
            )
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context).hasSingleBean(SecurityStartupValidator::class.java)
            }
    }

    @Test
    fun `should allow startup when captcha verification is disabled`() {
        contextRunner
            .withPropertyValues(
                "knowledgebase.security.admin-api-key=secret-key",
                "knowledgebase.recruiter-access.enabled=true",
                "knowledgebase.recruiter-access.token-secret=super-secret",
                "knowledgebase.recruiter-access.frontend-base-url=https://portfolio.example.com",
                "knowledgebase.recruiter-access.request-invitation-enabled=true",
                "knowledgebase.recruiter-access.captcha.provider=RECAPTCHA_V3",
                "knowledgebase.recruiter-access.captcha.verify-enabled=false"
            )
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context).hasSingleBean(SecurityStartupValidator::class.java)
            }
    }

    @Test
    fun `should fail when recruiter access email provider is brevo without template id`() {
        contextRunner
            .withPropertyValues(
                "knowledgebase.security.admin-api-key=secret-key",
                "knowledgebase.recruiter-access.enabled=true",
                "knowledgebase.recruiter-access.token-secret=super-secret",
                "knowledgebase.recruiter-access.frontend-base-url=https://portfolio.example.com",
                "knowledgebase.recruiter-access.email.provider=BREVO",
                "knowledgebase.recruiter-access.email.from-email=noreply@example.com",
                "knowledgebase.recruiter-access.email.brevo.api-key=brevo-secret"
            )
            .run { context ->
                assertThat(context.startupFailure).isNotNull
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalStateException::class.java)
                    .hasStackTraceContaining("knowledgebase.recruiter-access.email.brevo.template-id")
            }
    }

    @Configuration
    @EnableConfigurationProperties(KnowledgeBaseProperties::class)
    internal class TestConfiguration {
        @Bean
        fun securityStartupValidator(
            knowledgeBaseProperties: KnowledgeBaseProperties,
            environment: Environment
        ): SecurityStartupValidator = SecurityStartupValidator(knowledgeBaseProperties, environment)
    }
}
