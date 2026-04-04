package com.knowledgebase.api.handler

import com.knowledgebase.application.service.RecruiterAccessException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Global exception handler for REST API.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    suspend fun handleResponseStatusException(
        ex: ResponseStatusException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "ResponseStatusException: ${ex.reason}" }

        return ResponseEntity
            .status(ex.statusCode)
            .body(ErrorResponse(
                status = ex.statusCode.value(),
                error = ex.statusCode.toString(),
                message = ex.reason ?: "An error occurred",
                path = exchange.request.path.value(),
                code = (ex as? RecruiterAccessException)?.code
            ))
    }

    @ExceptionHandler(AuthenticationException::class)
    suspend fun handleAuthenticationException(
        ex: AuthenticationException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "AuthenticationException: ${ex.message}" }

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(
                status = 401,
                error = "Unauthorized",
                message = "Authentication required",
                path = exchange.request.path.value(),
                code = null
            ))
    }

    @ExceptionHandler(AccessDeniedException::class)
    suspend fun handleAccessDeniedException(
        ex: AccessDeniedException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "AccessDeniedException: ${ex.message}" }

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(
                status = 403,
                error = "Forbidden",
                message = "Access denied",
                path = exchange.request.path.value(),
                code = null
            ))
    }

    @ExceptionHandler(WebExchangeBindException::class)
    suspend fun handleValidationException(
        ex: WebExchangeBindException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        logger.warn { "Validation error: $errors" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                status = 400,
                error = "Bad Request",
                message = "Validation failed",
                path = exchange.request.path.value(),
                code = null,
                details = errors
            ))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    suspend fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "IllegalArgumentException: ${ex.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                status = 400,
                error = "Bad Request",
                message = ex.message ?: "Invalid argument",
                path = exchange.request.path.value(),
                code = null
            ))
    }

    @ExceptionHandler(Exception::class)
    suspend fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "Unhandled exception" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                status = 500,
                error = "Internal Server Error",
                message = "An unexpected error occurred",
                path = exchange.request.path.value(),
                code = null
            ))
    }
}

/**
 * Standard error response format.
 */
data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val code: String? = null,
    val details: Map<String, String>? = null
)
