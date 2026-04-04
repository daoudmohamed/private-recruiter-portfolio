package com.mutuelle.ragagent.api.controller

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.crypto.SecretKey

/**
 * Development-only controller for generating test JWT tokens.
 * DO NOT USE IN PRODUCTION.
 */
@RestController
@RequestMapping("/api/dev/auth")
@Profile("dev")
@Tag(name = "Dev Auth", description = "Development authentication endpoints (dev only)")
class DevAuthController(
    @Value("\${mutuelle.dev.jwt-secret:mutuelle-dev-secret-key-for-testing-only-min-256-bits!!}")
    private val jwtSecret: String
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    /**
     * Generates a test JWT token for development.
     */
    @PostMapping("/token")
    @Operation(summary = "Generate a test JWT token (dev only)")
    fun generateToken(@RequestBody request: TokenRequest): TokenResponse {
        val now = Date()
        val expiration = Date(now.time + (request.expirationHours * 3600 * 1000))

        val token = Jwts.builder()
            .subject(request.adherentId)
            .claim("adherent_id", request.adherentId)
            .claim("email", request.email ?: "${request.adherentId}@test.mutuelle.fr")
            .claim("name", request.name ?: "Test User")
            .claim("roles", listOf("USER"))
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()

        return TokenResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = request.expirationHours * 3600,
            adherentId = request.adherentId
        )
    }

    /**
     * Generates a test JWT token with default values.
     */
    @GetMapping("/token/quick")
    @Operation(summary = "Generate a quick test token with defaults (dev only)")
    fun quickToken(
        @RequestParam(defaultValue = "550e8400-e29b-41d4-a716-446655440001") adherentId: String
    ): TokenResponse {
        return generateToken(
            TokenRequest(
                adherentId = adherentId,
                expirationHours = 24
            )
        )
    }
}

data class TokenRequest(
    val adherentId: String,
    val email: String? = null,
    val name: String? = null,
    val expirationHours: Long = 24
)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val adherentId: String
)
