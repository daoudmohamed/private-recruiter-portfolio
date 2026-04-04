package com.knowledgebase.api.filter

import com.knowledgebase.application.service.RecruiterAccessSessionService
import com.knowledgebase.config.KnowledgeBaseProperties
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.server.PathContainer
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.util.pattern.PathPatternParser
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class RecruiterAccessFilter(
    private val knowledgeBaseProperties: KnowledgeBaseProperties,
    private val recruiterAccessSessionService: RecruiterAccessSessionService
) : WebFilter {

    private val pathPatternParser = PathPatternParser.defaultInstance

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!knowledgeBaseProperties.recruiterAccess.enabled) {
            return chain.filter(exchange)
        }

        val path = exchange.request.path.value()
        if (shouldSkip(path) || isAdminPath(path)) {
            return chain.filter(exchange)
        }

        return mono {
            recruiterAccessSessionService.resolveSession(exchange)
        }.flatMap {
            chain.filter(exchange)
        }.switchIfEmpty(
            Mono.defer {
                logger.warn { "Recruiter access denied for path: $path" }
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                exchange.response.setComplete()
            }
        )
    }

    private fun shouldSkip(path: String): Boolean {
        val pathContainer = exchangePath(path)
        return knowledgeBaseProperties.security.publicPaths.any { pattern ->
            pathPatternParser.parse(pattern).matches(pathContainer)
        }
    }

    private fun isAdminPath(path: String): Boolean {
        val pathContainer = exchangePath(path)
        return knowledgeBaseProperties.security.adminPaths.any { pattern ->
            pathPatternParser.parse(pattern).matches(pathContainer)
        }
    }

    private fun exchangePath(path: String): PathContainer = PathContainer.parsePath(path)
}
