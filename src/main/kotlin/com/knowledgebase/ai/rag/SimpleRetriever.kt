package com.knowledgebase.ai.rag

import com.knowledgebase.config.KnowledgeBaseProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Simple retriever for vector search.
 */
@Component
class SimpleRetriever(
    private val vectorStore: VectorStore,
    private val knowledgeBaseProperties: KnowledgeBaseProperties
) {
    private val similarityThreshold = knowledgeBaseProperties.rag.similarityThreshold
    private val topK = knowledgeBaseProperties.rag.topK

    /**
     * Retrieve relevant documents for a query.
     */
    suspend fun retrieve(query: String, topKOverride: Int? = null): RetrievalResult {
        val k = topKOverride ?: topK

        logger.debug { "Retrieving documents for query: $query (topK=$k, threshold=$similarityThreshold)" }

        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(k)
            .similarityThreshold(similarityThreshold)
            .build()

        logger.debug { "SearchRequest: query='$query', topK=$k, threshold=$similarityThreshold" }

        val documents = try {
            // Blocking operation, run on IO dispatcher
            withContext(Dispatchers.IO) {
                vectorStore.similaritySearch(searchRequest)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during vector search" }
            emptyList()
        }

        logger.info { "Retrieved ${documents.size} documents" }
        documents.forEachIndexed { index, doc ->
            logger.debug { "Document $index: score=${doc.metadata["distance"]}, source=${doc.metadata["source"]}" }
        }

        return RetrievalResult(documents)
    }
}

/**
 * Result of a retrieval operation.
 */
data class RetrievalResult(
    val documents: List<Document>
) {
    /**
     * Convert documents to a context string for the LLM.
     */
    fun toContextString(): String {
        if (documents.isEmpty()) {
            return "Aucun document pertinent trouvé pour cette question."
        }

        return documents.joinToString("\n\n---\n\n") { doc ->
            val source = doc.metadata["source"] as? String ?: "inconnu"
            val type = doc.metadata["type"] as? String ?: ""
            val header = if (type.isNotBlank()) {
                "Source: $source ($type)"
            } else {
                "Source: $source"
            }
            "$header\n${doc.text}"
        }
    }
}
