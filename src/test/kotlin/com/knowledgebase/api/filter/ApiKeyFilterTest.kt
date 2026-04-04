package com.knowledgebase.api.filter

import com.knowledgebase.config.KnowledgeBaseProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class ApiKeyFilterTest {

    @Test
    fun `should allow protected request when api key matches`() {
        val filter = ApiKeyFilter(
            KnowledgeBaseProperties(
                security = KnowledgeBaseProperties.Security(apiKey = "secret-key")
            )
        )
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/chat")
                .header("X-API-Key", "secret-key")
                .build()
        )
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isTrue()
        assertThat(exchange.response.statusCode).isNull()
    }

    @Test
    fun `should reject protected request when api key is missing`() {
        val filter = ApiKeyFilter(
            KnowledgeBaseProperties(
                security = KnowledgeBaseProperties.Security(apiKey = "secret-key")
            )
        )
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/chat").build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isFalse()
        assertThat(exchange.response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should allow request when api key security is disabled`() {
        val filter = ApiKeyFilter(KnowledgeBaseProperties())
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/chat").build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isTrue()
    }

    @Test
    fun `should bypass authentication for public endpoints`() {
        val filter = ApiKeyFilter(
            KnowledgeBaseProperties(
                security = KnowledgeBaseProperties.Security(
                    apiKey = "secret-key",
                    publicPaths = listOf("/actuator/health/**")
                )
            )
        )
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isTrue()
        assertThat(exchange.response.statusCode).isNull()
    }

    @Test
    fun `should bypass authentication for configured custom public path`() {
        val filter = ApiKeyFilter(
            KnowledgeBaseProperties(
                security = KnowledgeBaseProperties.Security(
                    apiKey = "secret-key",
                    publicPaths = listOf("/public/**")
                )
            )
        )
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/public/docs").build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isTrue()
        assertThat(exchange.response.statusCode).isNull()
    }

    @Test
    fun `should bypass authentication for frontend root path`() {
        val filter = ApiKeyFilter(
            KnowledgeBaseProperties(
                security = KnowledgeBaseProperties.Security(
                    apiKey = "secret-key",
                    publicPaths = listOf("/")
                )
            )
        )
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
        val chain = RecordingChain()

        filter.filter(exchange, chain).block()

        assertThat(chain.called).isTrue()
        assertThat(exchange.response.statusCode).isNull()
    }

    private class RecordingChain : WebFilterChain {
        var called: Boolean = false

        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            called = true
            return Mono.empty()
        }
    }
}
