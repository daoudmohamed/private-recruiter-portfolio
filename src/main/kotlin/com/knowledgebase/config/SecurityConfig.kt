package com.knowledgebase.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.server.PathContainer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository
import org.springframework.security.web.server.csrf.CsrfToken
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.util.pattern.PathPatternParser
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Mono

/**
 * Security configuration - API Key auth handled by ApiKeyFilter.
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val knowledgeBaseProperties: KnowledgeBaseProperties
) {
    private val pathPatternParser = PathPatternParser.defaultInstance
    private val csrfExemptPaths = listOf(
        "/api/v1/recruiter-access/request-invitation",
        "/api/v1/recruiter-access/consume"
    )

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        val publicPaths = knowledgeBaseProperties.security.publicPaths.toTypedArray()
        val csrfTokenRepository = CookieServerCsrfTokenRepository.withHttpOnlyFalse().apply {
            setCookiePath("/")
        }
        val csrfTokenRequestHandler = ServerCsrfTokenRequestAttributeHandler()
        return http
            .csrf { csrf ->
                csrf.csrfTokenRepository(csrfTokenRepository)
                csrf.csrfTokenRequestHandler(csrfTokenRequestHandler)
                csrf.requireCsrfProtectionMatcher(csrfProtectionMatcher())
            }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers(*publicPaths).permitAll()
                    .anyExchange().permitAll()  // API key checked by ApiKeyFilter
            }
            .build()
    }

    @Bean
    fun csrfProtectionMatcher(): ServerWebExchangeMatcher = ServerWebExchangeMatcher { exchange ->
        val method = exchange.request.method
        if (method == null || method in setOf(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE)) {
            return@ServerWebExchangeMatcher ServerWebExchangeMatcher.MatchResult.notMatch()
        }

        if (hasStatelessApiKeyAuthentication(exchange)) {
            return@ServerWebExchangeMatcher ServerWebExchangeMatcher.MatchResult.notMatch()
        }

        if (isCsrfExemptPath(exchange.request.path.value())) {
            return@ServerWebExchangeMatcher ServerWebExchangeMatcher.MatchResult.notMatch()
        }

        ServerWebExchangeMatcher.MatchResult.match()
    }

    @Bean
    fun csrfCookieWebFilter(): WebFilter = WebFilter { exchange, chain ->
        @Suppress("UNCHECKED_CAST")
        val csrfToken = exchange.attributes[CsrfToken::class.java.name] as? Mono<CsrfToken>
        csrfToken?.subscribe()
        chain.filter(exchange)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val corsProperties = knowledgeBaseProperties.security.cors
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = corsProperties.allowedOrigins
        configuration.allowedMethods = corsProperties.allowedMethods
        configuration.allowedHeaders = corsProperties.allowedHeaders
        configuration.allowCredentials = corsProperties.allowCredentials
        configuration.maxAge = corsProperties.maxAgeSeconds

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    private fun hasStatelessApiKeyAuthentication(exchange: org.springframework.web.server.ServerWebExchange): Boolean {
        val providedKey = exchange.request.headers.getFirst("X-API-Key")?.trim().orEmpty()
        if (providedKey.isBlank()) {
            return false
        }

        val effectiveAdminApiKey = knowledgeBaseProperties.security.adminApiKey.ifBlank {
            knowledgeBaseProperties.security.apiKey
        }

        return effectiveAdminApiKey.isNotBlank() && providedKey == effectiveAdminApiKey
    }

    private fun isCsrfExemptPath(path: String): Boolean {
        val pathContainer = PathContainer.parsePath(path)
        return csrfExemptPaths.any { pattern ->
            pathPatternParser.parse(pattern).matches(pathContainer)
        }
    }
}
