package com.knowledgebase.api.filter

import com.knowledgebase.config.KnowledgeBaseProperties
import mu.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.server.PathContainer
import org.springframework.stereotype.Component
import org.springframework.web.util.pattern.PathPatternParser
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
    private val knowledgeBaseProperties: KnowledgeBaseProperties
) : WebFilter {

    private val pathPatternParser = PathPatternParser.defaultInstance

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        val apiKey = effectiveAdminApiKey()

        // Skip auth for public endpoints
        if (shouldSkip(path)) {
            return chain.filter(exchange)
        }

        if (!requiresAdminApiKey(path)) {
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
        val pathContainer = exchangePath(path)
        return knowledgeBaseProperties.security.publicPaths.any { pattern ->
            pathPatternParser.parse(pattern).matches(pathContainer)
        }
    }

    private fun requiresAdminApiKey(path: String): Boolean {
        val pathContainer = exchangePath(path)
        return when {
            knowledgeBaseProperties.recruiterAccess.enabled ->
                knowledgeBaseProperties.security.adminPaths.any { pattern ->
                    pathPatternParser.parse(pattern).matches(pathContainer)
                }
            else -> true
        }
    }

    private fun effectiveAdminApiKey(): String =
        knowledgeBaseProperties.security.adminApiKey.ifBlank {
            knowledgeBaseProperties.security.apiKey
        }

    private fun exchangePath(path: String): PathContainer = PathContainer.parsePath(path)
}
