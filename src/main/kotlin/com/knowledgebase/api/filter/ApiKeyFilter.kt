package com.knowledgebase.api.filter

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

/**
 * Filter for API key authentication.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiKeyFilter(
    @Value("\${knowledgebase.security.api-key:}")
    private val apiKey: String
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()

        // Skip auth for public endpoints
        if (shouldSkip(path)) {
            return chain.filter(exchange)
        }

        // Skip if no API key configured (development mode)
        if (apiKey.isBlank()) {
            logger.debug { "No API key configured, allowing request to: $path" }
            return chain.filter(exchange)
        }

        // Check API key
        val providedKey = exchange.request.headers.getFirst("X-API-Key")
            ?: exchange.request.headers.getFirst("Authorization")?.removePrefix("Bearer ")

        if (providedKey == apiKey) {
            logger.debug { "Valid API key for request to: $path" }
            return chain.filter(exchange)
        }

        // Unauthorized
        logger.warn { "Unauthorized request to: $path (invalid or missing API key)" }
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }

    private fun shouldSkip(path: String): Boolean {
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/webjars") ||
               path == "/swagger-ui.html"
    }
}
