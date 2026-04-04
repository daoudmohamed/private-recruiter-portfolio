package com.knowledgebase.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository
import org.springframework.security.web.server.csrf.CsrfToken
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
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
}
