package com.knowledgebase.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

class SecurityConfigTest {

    @Test
    fun `should build cors configuration from properties`() {
        val properties = KnowledgeBaseProperties(
            security = KnowledgeBaseProperties.Security(
                apiKey = "secret-key",
                cors = KnowledgeBaseProperties.Security.Cors(
                    allowedOrigins = listOf("https://app.example.com"),
                    allowedMethods = listOf("GET", "POST"),
                    allowedHeaders = listOf("Authorization"),
                    allowCredentials = false,
                    maxAgeSeconds = 120
                )
            )
        )

        val source = SecurityConfig(properties).corsConfigurationSource() as UrlBasedCorsConfigurationSource
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/api/v1/chat").build())
        val configuration = source.getCorsConfiguration(exchange)

        assertThat(configuration).isNotNull
        assertThat(configuration!!.allowedOrigins).containsExactly("https://app.example.com")
        assertThat(configuration.allowedMethods).containsExactly("GET", "POST")
        assertThat(configuration.allowedHeaders).containsExactly("Authorization")
        assertThat(configuration.allowCredentials).isFalse()
        assertThat(configuration.maxAge).isEqualTo(120)
    }
}
