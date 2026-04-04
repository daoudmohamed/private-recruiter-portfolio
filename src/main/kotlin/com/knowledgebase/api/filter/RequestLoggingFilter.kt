package com.knowledgebase.api.filter

import mu.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * WebFilter that logs incoming requests and response times.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class RequestLoggingFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val startTime = Instant.now()
        val request = exchange.request
        val path = request.path.value()
        val method = request.method.name()

        // Skip logging for health checks and static resources
        if (shouldSkipLogging(path)) {
            return chain.filter(exchange)
        }

        val requestId = exchange.request.headers.getFirst("X-Request-ID")
            ?: java.util.UUID.randomUUID().toString().take(8)

        // Add request ID to response headers
        exchange.response.headers.add("X-Request-ID", requestId)

        logger.info { "[$requestId] --> $method $path" }

        return chain.filter(exchange)
            .doFinally { _ ->
                val duration = Duration.between(startTime, Instant.now())
                val status = exchange.response.statusCode?.value() ?: 0
                logger.info { "[$requestId] <-- $method $path $status (${duration.toMillis()}ms)" }
            }
    }

    private fun shouldSkipLogging(path: String): Boolean {
        return path.startsWith("/actuator/health") ||
                path.startsWith("/webjars") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".ico")
    }
}
