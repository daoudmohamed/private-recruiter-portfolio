package com.mutuelle.ragagent.ai.rag

import com.mutuelle.ragagent.domain.intent.Intent
import mu.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Retriever for FAQ and knowledge base documents.
 * Filters results based on detected intent.
 */
@Component
class FaqRetriever(
    private val vectorStore: VectorStore,
    @Value("\${mutuelle.rag.similarity-threshold:0.65}")
    private val similarityThreshold: Double,
    @Value("\${mutuelle.rag.top-k:5}")
    private val defaultTopK: Int
) {
    /**
     * Retrieves FAQ documents relevant to the query.
     * Filters by intent category when provided.
     */
    suspend fun retrieve(
        query: String,
        intent: Intent? = null,
        topK: Int = defaultTopK
    ): List<Document> {
        logger.debug { "Retrieving FAQ for query: '$query', intent: $intent" }

        val searchRequestBuilder = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)

        // Add category filter based on intent
        intent?.let { detectedIntent ->
            val categoryFilter = mapIntentToCategory(detectedIntent)
            if (categoryFilter != null) {
                val filterExpression = FilterExpressionBuilder()
                    .eq("category", categoryFilter)
                    .build()
                searchRequestBuilder.filterExpression(filterExpression)
            }
        }

        val results = vectorStore.similaritySearch(searchRequestBuilder.build())
        logger.debug { "Retrieved ${results?.size} FAQ documents" }

        return results!!
    }

    /**
     * Retrieves FAQ documents with multiple category filters.
     */
    suspend fun retrieveMultiCategory(
        query: String,
        categories: List<String>,
        topK: Int = defaultTopK
    ): List<Document> {
        logger.debug { "Retrieving FAQ for categories: $categories" }

        val searchRequestBuilder = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)

        if (categories.isNotEmpty()) {
            val filterExpression = FilterExpressionBuilder()
                .`in`("category", categories)
                .build()
            searchRequestBuilder.filterExpression(filterExpression)
        }

        return vectorStore.similaritySearch(searchRequestBuilder.build())!!
    }

    /**
     * Retrieves documents from a specific source (faq, guarantee, policy).
     */
    suspend fun retrieveBySource(
        query: String,
        source: String,
        topK: Int = defaultTopK
    ): List<Document> {
        val filterExpression = FilterExpressionBuilder()
            .eq("source", source)
            .build()

        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .filterExpression(filterExpression)
            .build()

        return vectorStore.similaritySearch(searchRequest)!!
    }

    /**
     * Maps intent to document category for filtering.
     */
    private fun mapIntentToCategory(intent: Intent): String? {
        return when (intent) {
            Intent.CONTRAT -> "CONTRAT"
            Intent.REMBOURSEMENT -> "REMBOURSEMENT"
            Intent.DEVIS -> "DEVIS"
            Intent.ADMINISTRATIF -> "ADMINISTRATIF"
            Intent.RECLAMATION -> "RECLAMATION"
            Intent.GENERAL -> null // No filter for general questions
            Intent.GREETING -> null
            Intent.ESCALATION -> null
            Intent.UNKNOWN -> null
        }
    }
}
