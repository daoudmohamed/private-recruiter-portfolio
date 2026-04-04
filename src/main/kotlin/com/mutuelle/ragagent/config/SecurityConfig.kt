package com.mutuelle.ragagent.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono
import javax.crypto.spec.SecretKeySpec

/**
 * Security configuration for OAuth2 JWT authentication.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Value("\${mutuelle.dev.jwt-secret:mutuelle-dev-secret-key-for-testing-only-min-256-bits!!}")
    private lateinit var devJwtSecret: String

    /**
     * Security filter chain for production environment.
     */
    //@Bean
    @Profile("!dev")
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints
                    .pathMatchers("/actuator/health/**").permitAll()
                    .pathMatchers("/actuator/info").permitAll()
                    .pathMatchers("/api-docs/**").permitAll()
                    .pathMatchers("/swagger-ui/**").permitAll()
                    .pathMatchers("/swagger-ui.html").permitAll()
                    .pathMatchers("/webjars/**").permitAll()
                    // Webhook endpoints (authenticated via secret header)
                    .pathMatchers("/api/v1/webhooks/**").permitAll()
                    // All other endpoints require authentication
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .build()
    }

    /**
     * Security filter chain for development environment (less restrictive).
     */
    @Bean
    @Profile("dev")
    fun devSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange { exchanges ->
                exchanges
                    // Allow all in dev for easier testing
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/api-docs/**").permitAll()
                    .pathMatchers("/swagger-ui/**").permitAll()
                    .pathMatchers("/swagger-ui.html").permitAll()
                    .pathMatchers("/webjars/**").permitAll()
                    .pathMatchers("/api/v1/webhooks/**").permitAll()
                    // Dev auth endpoint - no auth required
                    .pathMatchers("/api/dev/**").permitAll()
                    // API endpoints require auth
                    .pathMatchers("/api/**").authenticated()
                    .anyExchange().permitAll()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .build()
    }

    /**
     * JWT decoder for development environment using symmetric key.
     * Uses HS384 to match JJWT's automatic algorithm selection for keys >= 384 bits.
     */
    @Bean
    @Profile("dev")
    fun devJwtDecoder(): ReactiveJwtDecoder {
        val secretKey = SecretKeySpec(devJwtSecret.toByteArray(), "HmacSHA384")
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS384)
            .build()
    }

    /**
     * CORS configuration for development.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    /**
     * Converts JWT claims to Spring Security authorities.
     */
    private fun jwtAuthenticationConverter(): Converter<Jwt, Mono<AbstractAuthenticationToken>> {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val authorities = mutableListOf<SimpleGrantedAuthority>()

            // Extract roles from JWT claims
            val roles = jwt.getClaimAsStringList("roles")
                ?: jwt.getClaimAsStringList("authorities")
                ?: emptyList()

            roles.forEach { role ->
                if (role.startsWith("ROLE_")) {
                    authorities.add(SimpleGrantedAuthority(role))
                } else {
                    authorities.add(SimpleGrantedAuthority("ROLE_$role"))
                }
            }

            // Default role for authenticated users
            if (authorities.isEmpty()) {
                authorities.add(SimpleGrantedAuthority("ROLE_USER"))
            }

            authorities.toList()
        }

        return ReactiveJwtAuthenticationConverterAdapter(converter)
    }
}
