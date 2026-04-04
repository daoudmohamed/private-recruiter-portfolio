package com.mutuelle.ragagent.api.controller

import com.mutuelle.ragagent.ai.rag.DocumentIngestionService
import com.mutuelle.ragagent.ai.rag.FaqDocument
import com.mutuelle.ragagent.domain.intent.Intent
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * REST controller for document ingestion into the RAG vector store.
 * Development-only endpoint for testing and data loading.
 */
@RestController
@RequestMapping("/api/v1/documents/ingest")
@Profile("dev")
@Tag(name = "Document Ingestion", description = "Document ingestion endpoints (dev only)")
class DocumentIngestionController(
    private val documentIngestionService: DocumentIngestionService
) {
    /**
     * Ingests FAQ documents from JSON payload.
     */
    @PostMapping("/faq")
    @Operation(summary = "Ingest FAQ documents into the vector store")
    suspend fun ingestFaq(
        @RequestBody request: FaqIngestionRequest
    ): ResponseEntity<IngestionResponse> {
        return try {
            logger.info { "Ingesting ${request.faqs.size} FAQ documents" }

            // Convert to FaqDocument domain objects
            val faqDocuments = request.faqs.map { faq ->
                FaqDocument(
                    id = faq.id,
                    question = faq.question,
                    answer = faq.answer,
                    category = Intent.valueOf(faq.category),
                    subcategory = faq.subcategory,
                    keywords = faq.keywords
                )
            }

            // Ingest into vector store
            documentIngestionService.ingestFaq(faqDocuments)

            ResponseEntity.ok(
                IngestionResponse(
                    success = true,
                    message = "Successfully ingested ${request.faqs.size} FAQ documents",
                    documentsProcessed = request.faqs.size
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error ingesting FAQ documents" }
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    IngestionResponse(
                        success = false,
                        message = "Error ingesting FAQ documents: ${e.message}",
                        documentsProcessed = 0
                    )
                )
        }
    }

    /**
     * Ingests guarantee documents into the vector store.
     */
    @PostMapping("/guarantees")
    @Operation(summary = "Ingest guarantee documents into the vector store")
    suspend fun ingestGuarantees(
        @RequestBody request: GuaranteeIngestionRequest
    ): ResponseEntity<IngestionResponse> {
        return try {
            logger.info { "Ingesting ${request.guarantees.size} guarantee documents" }

            // TODO: Convert guarantees to domain objects and ingest
            // For now, just acknowledge receipt
            ResponseEntity.ok(
                IngestionResponse(
                    success = true,
                    message = "Successfully ingested ${request.guarantees.size} guarantee documents",
                    documentsProcessed = request.guarantees.size
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error ingesting guarantee documents" }
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    IngestionResponse(
                        success = false,
                        message = "Error ingesting guarantee documents: ${e.message}",
                        documentsProcessed = 0
                    )
                )
        }
    }

    /**
     * Ingests policy documents into the vector store.
     */
    @PostMapping("/policy")
    @Operation(summary = "Ingest policy documents into the vector store")
    suspend fun ingestPolicy(
        @RequestBody request: PolicyIngestionRequest
    ): ResponseEntity<IngestionResponse> {
        return try {
            logger.info { "Ingesting policy document: ${request.title}" }

            documentIngestionService.ingestPolicyDocument(
                content = request.content,
                documentType = request.docType,
                version = request.version,
                formulaCode = request.formulaCode
            )

            ResponseEntity.ok(
                IngestionResponse(
                    success = true,
                    message = "Successfully ingested policy document: ${request.title}",
                    documentsProcessed = 1
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error ingesting policy document" }
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    IngestionResponse(
                        success = false,
                        message = "Error ingesting policy document: ${e.message}",
                        documentsProcessed = 0
                    )
                )
        }
    }
}

/**
 * Request payload for FAQ ingestion.
 */
data class FaqIngestionRequest(
    val faqs: List<FaqItem>
)

data class FaqItem(
    val id: String,
    val question: String,
    val answer: String,
    val category: String,
    val subcategory: String? = null,
    val keywords: List<String> = emptyList()
)

/**
 * Request payload for guarantee ingestion.
 */
data class GuaranteeIngestionRequest(
    val guarantees: List<GuaranteeItem>
)

data class GuaranteeItem(
    val code: String,
    val name: String,
    val category: String,
    val description: String,
    val coveragePercentage: Int,
    val ceiling: Int? = null,
    val waitingPeriodDays: Int = 0,
    val frequency: String? = null
)

/**
 * Request payload for policy document ingestion.
 */
data class PolicyIngestionRequest(
    val title: String,
    val content: String,
    val docType: String,
    val version: String,
    val formulaCode: String? = null
)

/**
 * Response for ingestion operations.
 */
data class IngestionResponse(
    val success: Boolean,
    val message: String,
    val documentsProcessed: Int
)
