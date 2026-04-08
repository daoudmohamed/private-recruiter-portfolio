package com.knowledgebase.api.filter

import com.knowledgebase.application.service.RecruiterAccessSessionService
import com.knowledgebase.config.KnowledgeBaseProperties
import com.knowledgebase.domain.model.RecruiterAccessSession
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Instant

class RecruiterAccessFilterTest {

    @Test
    fun `should allow request when recruiter access is disabled`() {
        val filter = RecruiterAccessFilter(
            KnowledgeBaseProperties(),
            mockk(relaxed = true)
        )
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/chat").build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isTrue()
    }

    @Test
    fun `should reject protected request without recruiter session`() {
        val sessionService = mockk<RecruiterAccessSessionService>()
        coEvery { sessionService.resolveSession(any()) } returns null
        val filter = RecruiterAccessFilter(
            KnowledgeBaseProperties(
                recruiterAccess = KnowledgeBaseProperties.RecruiterAccess(
                    enabled = true,
                    tokenSecret = "secret"
                )
            ),
            sessionService
        )
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/chat").build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isFalse()
        assertThat(exchange.response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should allow protected request with recruiter session`() {
        val sessionService = mockk<RecruiterAccessSessionService>()
        coEvery { sessionService.resolveSession(any()) } returns RecruiterAccessSession(
            id = "session-1",
            email = "recruteur@example.com",
            invitationId = "invite-1",
            expiresAt = Instant.now().plusSeconds(3600)
        )
        val filter = RecruiterAccessFilter(
            KnowledgeBaseProperties(
                recruiterAccess = KnowledgeBaseProperties.RecruiterAccess(
                    enabled = true,
                    tokenSecret = "secret"
                )
            ),
            sessionService
        )
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/chat").cookie(HttpCookie("kb_recruiter_session", "session-1")).build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isTrue()
    }

    @Test
    fun `should allow frontend root path without recruiter session`() {
        val sessionService = mockk<RecruiterAccessSessionService>()
        val filter = RecruiterAccessFilter(
            KnowledgeBaseProperties(
                security = KnowledgeBaseProperties.Security(
                    publicPaths = listOf("/")
                ),
                recruiterAccess = KnowledgeBaseProperties.RecruiterAccess(
                    enabled = true,
                    tokenSecret = "secret"
                )
            ),
            sessionService
        )
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isTrue()
        assertThat(exchange.response.statusCode).isNull()
    }

    @Test
    fun `should allow public branding assets without recruiter session`() {
        val sessionService = mockk<RecruiterAccessSessionService>()
        val filter = RecruiterAccessFilter(
            KnowledgeBaseProperties(
                security = KnowledgeBaseProperties.Security(),
                recruiterAccess = KnowledgeBaseProperties.RecruiterAccess(
                    enabled = true,
                    tokenSecret = "secret"
                )
            ),
            sessionService
        )

        val faviconExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/favicon.svg").build())
        val brandMarkExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/brand-mark.svg").build())
        val faviconChain = RecordingChain()
        val brandMarkChain = RecordingChain()

        filter.filter(faviconExchange, faviconChain).block()
        filter.filter(brandMarkExchange, brandMarkChain).block()

        assertThat(faviconChain.called).isTrue()
        assertThat(brandMarkChain.called).isTrue()
        assertThat(faviconExchange.response.statusCode).isNull()
        assertThat(brandMarkExchange.response.statusCode).isNull()
    }

    private class RecordingChain : WebFilterChain {
        var called: Boolean = false

        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            called = true
            return Mono.empty()
        }
    }
}
