package com.mutuelle.ragagent.ai.rag

import com.mutuelle.ragagent.ai.context.AdherentContext
import com.mutuelle.ragagent.domain.intent.Intent
import mu.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.stereotype.Component
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * Hybrid retriever that combines FAQ/knowledge base retrieval
 * with adherent-specific data retrieval.
 */
@Component
class HybridRetriever(
    private val faqRetriever: FaqRetriever,
    private val adherentDataRetriever: AdherentDataRetriever
) {
    /**
     * Performs hybrid retrieval combining FAQ and adherent data.
     */
    suspend fun retrieve(
        query: String,
        adherentContext: AdherentContext?,
        intent: Intent,
        topK: Int = 5
    ): HybridRetrievalResult = coroutineScope {
        logger.debug { "Hybrid retrieval for query: '$query', intent: $intent" }

        // Parallel retrieval of FAQ and adherent-specific data
        val faqDeferred = async {
            faqRetriever.retrieve(query, intent, topK)
        }

        val adherentDocsDeferred = async {
            if (adherentContext != null && intent.requiresAdherentData) {
                adherentDataRetriever.retrieve(adherentContext, intent)
            } else {
                emptyList()
            }
        }

        val faqDocs = faqDeferred.await()
        val adherentDocs = adherentDocsDeferred.await()

        // Merge and re-rank results
        val mergedDocs = mergeAndRerank(faqDocs, adherentDocs, query)

        HybridRetrievalResult(
            faqDocuments = faqDocs,
            adherentDocuments = adherentDocs,
            mergedDocuments = mergedDocs,
            intent = intent
        )
    }

    /**
     * Merges FAQ and adherent documents, prioritizing adherent-specific data
     * for personalized responses.
     */
    private fun mergeAndRerank(
        faqDocs: List<Document>,
        adherentDocs: List<Document>,
        query: String
    ): List<Document> {
        // Prioritize adherent-specific documents
        val result = mutableListOf<Document>()

        // Add adherent documents first (most relevant for personalization)
        result.addAll(adherentDocs)

        // Add FAQ documents that don't overlap
        faqDocs.forEach { faqDoc ->
            if (!result.any { it.id == faqDoc.id }) {
                result.add(faqDoc)
            }
        }

        // Limit total documents
        return result.take(10)
    }
}

/**
 * Result of hybrid retrieval.
 */
data class HybridRetrievalResult(
    val faqDocuments: List<Document>,
    val adherentDocuments: List<Document>,
    val mergedDocuments: List<Document>,
    val intent: Intent
) {
    val totalDocuments: Int
        get() = mergedDocuments.size

    fun toContextString(): String {
        return mergedDocuments.joinToString("\n\n---\n\n") { doc ->
            doc.text ?: ""
        }
    }
}
