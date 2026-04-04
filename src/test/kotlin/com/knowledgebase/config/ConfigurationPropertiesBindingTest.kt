package com.knowledgebase.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ConfigurationPropertiesBindingTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration::class.java)

    @Test
    fun `should bind knowledgebase properties`() {
        contextRunner
            .withPropertyValues(
                "knowledgebase.security.api-key=test-key",
                "knowledgebase.security.admin-api-key=admin-key",
                "knowledgebase.security.public-paths[0]=/health",
                "knowledgebase.security.public-paths[1]=/docs/**",
                "knowledgebase.security.admin-paths[0]=/admin/**",
                "knowledgebase.security.cors.allowed-origins[0]=https://app.example.com",
                "knowledgebase.security.cors.allowed-methods[0]=GET",
                "knowledgebase.security.cors.allowed-methods[1]=POST",
                "knowledgebase.security.cors.allowed-headers[0]=Authorization",
                "knowledgebase.security.cors.allow-credentials=false",
                "knowledgebase.security.cors.max-age-seconds=120",
                "knowledgebase.recruiter-access.enabled=true",
                "knowledgebase.recruiter-access.request-invitation-enabled=true",
                "knowledgebase.recruiter-access.invitation-ttl-hours=24",
                "knowledgebase.recruiter-access.session-ttl-hours=12",
                "knowledgebase.recruiter-access.cookie-name=recruiter_session",
                "knowledgebase.recruiter-access.frontend-base-url=https://portfolio.example.com",
                "knowledgebase.recruiter-access.token-secret=super-secret",
                "knowledgebase.recruiter-access.captcha.provider=RECAPTCHA",
                "knowledgebase.recruiter-access.captcha.site-key=site-key",
                "knowledgebase.recruiter-access.captcha.verify-enabled=false",
                "knowledgebase.recruiter-access.captcha.recaptcha.secret-key=captcha-secret",
                "knowledgebase.recruiter-access.rate-limit.max-requests-per-ip=7",
                "knowledgebase.recruiter-access.rate-limit.max-requests-per-email=4",
                "knowledgebase.recruiter-access.rate-limit.window-minutes=30",
                "knowledgebase.recruiter-access.email.provider=BREVO",
                "knowledgebase.recruiter-access.email.from-email=noreply@example.com",
                "knowledgebase.recruiter-access.email.from-name=Portfolio",
                "knowledgebase.recruiter-access.email.subject=Acces prive",
                "knowledgebase.recruiter-access.email.brevo.api-key=brevo-secret",
                "knowledgebase.recruiter-access.email.brevo.template-id=42",
                "knowledgebase.recruiter-access.email.brevo.reply-to=reply@example.com",
                "knowledgebase.chat.max-history-messages=42",
                "knowledgebase.chat.session-timeout-hours=12",
                "knowledgebase.rag.similarity-threshold=0.81",
                "knowledgebase.rag.top-k=9",
                "knowledgebase.documents.folder=/tmp/docs",
                "knowledgebase.documents.scan-on-startup=false"
            )
            .run { context ->
                val properties = context.getBean(KnowledgeBaseProperties::class.java)

                assertThat(properties.security.apiKey).isEqualTo("test-key")
                assertThat(properties.security.adminApiKey).isEqualTo("admin-key")
                assertThat(properties.security.publicPaths).containsExactly("/health", "/docs/**")
                assertThat(properties.security.adminPaths).containsExactly("/admin/**")
                assertThat(properties.security.cors.allowedOrigins).containsExactly("https://app.example.com")
                assertThat(properties.security.cors.allowedMethods).containsExactly("GET", "POST")
                assertThat(properties.security.cors.allowedHeaders).containsExactly("Authorization")
                assertThat(properties.security.cors.allowCredentials).isFalse()
                assertThat(properties.security.cors.maxAgeSeconds).isEqualTo(120)
                assertThat(properties.recruiterAccess.enabled).isTrue()
                assertThat(properties.recruiterAccess.requestInvitationEnabled).isTrue()
                assertThat(properties.recruiterAccess.invitationTtlHours).isEqualTo(24)
                assertThat(properties.recruiterAccess.sessionTtlHours).isEqualTo(12)
                assertThat(properties.recruiterAccess.cookieName).isEqualTo("recruiter_session")
                assertThat(properties.recruiterAccess.frontendBaseUrl).isEqualTo("https://portfolio.example.com")
                assertThat(properties.recruiterAccess.tokenSecret).isEqualTo("super-secret")
                assertThat(properties.recruiterAccess.captcha.provider).isEqualTo(KnowledgeBaseProperties.RecruiterAccess.Captcha.Provider.RECAPTCHA)
                assertThat(properties.recruiterAccess.captcha.siteKey).isEqualTo("site-key")
                assertThat(properties.recruiterAccess.captcha.verifyEnabled).isFalse()
                assertThat(properties.recruiterAccess.captcha.recaptcha.secretKey).isEqualTo("captcha-secret")
                assertThat(properties.recruiterAccess.rateLimit.maxRequestsPerIp).isEqualTo(7)
                assertThat(properties.recruiterAccess.rateLimit.maxRequestsPerEmail).isEqualTo(4)
                assertThat(properties.recruiterAccess.rateLimit.windowMinutes).isEqualTo(30)
                assertThat(properties.recruiterAccess.email.provider).isEqualTo(KnowledgeBaseProperties.RecruiterAccess.Email.Provider.BREVO)
                assertThat(properties.recruiterAccess.email.fromEmail).isEqualTo("noreply@example.com")
                assertThat(properties.recruiterAccess.email.fromName).isEqualTo("Portfolio")
                assertThat(properties.recruiterAccess.email.subject).isEqualTo("Acces prive")
                assertThat(properties.recruiterAccess.email.brevo.apiKey).isEqualTo("brevo-secret")
                assertThat(properties.recruiterAccess.email.brevo.templateId).isEqualTo(42)
                assertThat(properties.recruiterAccess.email.brevo.replyTo).isEqualTo("reply@example.com")
                assertThat(properties.chat.maxHistoryMessages).isEqualTo(42)
                assertThat(properties.chat.sessionTimeoutHours).isEqualTo(12)
                assertThat(properties.rag.similarityThreshold).isEqualTo(0.81)
                assertThat(properties.rag.topK).isEqualTo(9)
                assertThat(properties.documents.folder).isEqualTo("/tmp/docs")
                assertThat(properties.documents.scanOnStartup).isFalse()
            }
    }

    @Test
    fun `should bind redis and qdrant properties`() {
        contextRunner
            .withPropertyValues(
                "spring.data.redis.host=redis.internal",
                "spring.data.redis.port=6380",
                "spring.data.redis.password=redis-secret",
                "spring.ai.vectorstore.qdrant.host=qdrant.internal",
                "spring.ai.vectorstore.qdrant.port=7334",
                "spring.ai.vectorstore.qdrant.api-key=qdrant-secret",
                "spring.ai.vectorstore.qdrant.collection-name=custom-collection",
                "spring.ai.vectorstore.qdrant.initialize-schema=false"
            )
            .run { context ->
                val redisProperties = context.getBean(RedisProperties::class.java)
                val qdrantProperties = context.getBean(QdrantProperties::class.java)

                assertThat(redisProperties.host).isEqualTo("redis.internal")
                assertThat(redisProperties.port).isEqualTo(6380)
                assertThat(redisProperties.password).isEqualTo("redis-secret")

                assertThat(qdrantProperties.host).isEqualTo("qdrant.internal")
                assertThat(qdrantProperties.port).isEqualTo(7334)
                assertThat(qdrantProperties.apiKey).isEqualTo("qdrant-secret")
                assertThat(qdrantProperties.collectionName).isEqualTo("custom-collection")
                assertThat(qdrantProperties.initializeSchema).isFalse()
            }
    }

    @Test
    fun `should reject invalid knowledgebase properties`() {
        contextRunner
            .withPropertyValues(
                "knowledgebase.chat.max-history-messages=0",
                "knowledgebase.rag.similarity-threshold=1.2",
                "knowledgebase.rag.top-k=21",
                "knowledgebase.security.public-paths=",
                "knowledgebase.security.admin-paths=",
                "knowledgebase.security.cors.allowed-origins=",
                "knowledgebase.security.cors.max-age-seconds=0",
                "knowledgebase.recruiter-access.invitation-ttl-hours=0",
                "knowledgebase.recruiter-access.session-ttl-hours=0",
                "knowledgebase.recruiter-access.rate-limit.max-requests-per-ip=0",
                "knowledgebase.recruiter-access.rate-limit.max-requests-per-email=0",
                "knowledgebase.recruiter-access.rate-limit.window-minutes=0",
                "knowledgebase.chat.session-timeout-hours=169",
                "knowledgebase.documents.folder=   "
            )
            .run { context ->
                assertThat(context.startupFailure).isNotNull
                assertThat(context.startupFailure)
                    .hasStackTraceContaining("knowledgebase.security.publicPaths")
                    .hasStackTraceContaining("knowledgebase.security.adminPaths")
                    .hasStackTraceContaining("knowledgebase.security.cors.allowedOrigins")
                    .hasStackTraceContaining("knowledgebase.security.cors.maxAgeSeconds")
                    .hasStackTraceContaining("knowledgebase.recruiterAccess.invitationTtlHours")
                    .hasStackTraceContaining("knowledgebase.recruiterAccess.sessionTtlHours")
                    .hasStackTraceContaining("knowledgebase.recruiterAccess.rateLimit.maxRequestsPerIp")
                    .hasStackTraceContaining("knowledgebase.recruiterAccess.rateLimit.maxRequestsPerEmail")
                    .hasStackTraceContaining("knowledgebase.recruiterAccess.rateLimit.windowMinutes")
                    .hasStackTraceContaining("knowledgebase.chat.maxHistoryMessages")
                    .hasStackTraceContaining("knowledgebase.chat.sessionTimeoutHours")
                    .hasStackTraceContaining("knowledgebase.rag.similarityThreshold")
                    .hasStackTraceContaining("knowledgebase.rag.topK")
                    .hasStackTraceContaining("knowledgebase.documents.folder")
            }
    }

    @Test
    fun `should reject invalid redis properties`() {
        contextRunner
            .withPropertyValues(
                "spring.data.redis.host=",
                "spring.data.redis.port=70000"
            )
            .run { context ->
                assertThat(context.startupFailure).isNotNull
                assertThat(context.startupFailure)
                    .hasStackTraceContaining("spring.data.redis.host")
                    .hasStackTraceContaining("spring.data.redis.port")
            }
    }

    @Test
    fun `should reject invalid qdrant properties`() {
        contextRunner
            .withPropertyValues(
                "spring.ai.vectorstore.qdrant.host=",
                "spring.ai.vectorstore.qdrant.port=0",
                "spring.ai.vectorstore.qdrant.collection-name="
            )
            .run { context ->
                assertThat(context.startupFailure).isNotNull
                assertThat(context.startupFailure)
                    .hasStackTraceContaining("spring.ai.vectorstore.qdrant.host")
                    .hasStackTraceContaining("spring.ai.vectorstore.qdrant.port")
                    .hasStackTraceContaining("spring.ai.vectorstore.qdrant.collection-name")
            }
    }

    @Configuration
    @EnableConfigurationProperties(
        value = [
            KnowledgeBaseProperties::class,
            RedisProperties::class,
            QdrantProperties::class
        ]
    )
    internal class TestConfiguration
}
