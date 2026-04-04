package com.mutuelle.ragagent.api.filter

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
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
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * WebFilter that applies rate limiting to API requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RateLimitFilter(
    @Value("\${mutuelle.rate-limit.requests-per-minute:100}")
    private val requestsPerMinute: Int,
    @Value("\${mutuelle.rate-limit.timeout-seconds:5}")
    private val timeoutSeconds: Long
) : WebFilter {

    private val rateLimiterRegistry: RateLimiterRegistry

    init {
        val config = RateLimiterConfig.custom()
            .limitForPeriod(requestsPerMinute)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(timeoutSeconds))
            .build()

        rateLimiterRegistry = RateLimiterRegistry.of(config)
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()

        // Skip rate limiting for health checks and public endpoints
        if (shouldSkipRateLimiting(path)) {
            return chain.filter(exchange)
        }

        // Get client identifier (IP or user ID)
        val clientId = getClientIdentifier(exchange)
        val rateLimiter = rateLimiterRegistry.rateLimiter(clientId)

        return if (rateLimiter.acquirePermission()) {
            chain.filter(exchange)
        } else {
            logger.warn { "Rate limit exceeded for client: $clientId on path: $path" }
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
            exchange.response.headers.add("Retry-After", "60")
            exchange.response.setComplete()
        }
    }

    private fun shouldSkipRateLimiting(path: String): Boolean {
        return path.startsWith("/actuator") ||
                path.startsWith("/swagger") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/webjars")
    }

    private fun getClientIdentifier(exchange: ServerWebExchange): String {
        // Try to get user ID from JWT first
        val principal = exchange.request.headers.getFirst("Authorization")
            ?.removePrefix("Bearer ")
            ?.let { token ->
                // Extract subject from JWT (simplified - in production use proper JWT parsing)
                try {
                    val parts = token.split(".")
                    if (parts.size >= 2) {
                        val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                        // Extract "sub" claim (simplified)
                        val subMatch = Regex("\"sub\":\"([^\"]+)\"").find(payload)
                        subMatch?.groupValues?.get(1)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

        // Fall back to IP address
        return principal ?: getClientIp(exchange)
    }

    private fun getClientIp(exchange: ServerWebExchange): String {
        val request = exchange.request

        // Check for forwarded headers (when behind a proxy)
        return request.headers.getFirst("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.headers.getFirst("X-Real-IP")
            ?: request.remoteAddress?.address?.hostAddress
            ?: "unknown"
    }
}
