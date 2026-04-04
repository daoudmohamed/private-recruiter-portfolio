package com.knowledgebase.config

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "spring.ai.vectorstore.qdrant")
data class QdrantProperties(
    @field:NotBlank
    val host: String = "localhost",
    @field:Min(1)
    @field:Max(65535)
    val port: Int = 6334,
    val apiKey: String = "",
    @field:NotBlank
    val collectionName: String = "knowledge-base",
    val initializeSchema: Boolean = true
)
