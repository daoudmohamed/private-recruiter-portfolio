package com.mutuelle.ragagent.ai.rag

import com.mutuelle.ragagent.domain.intent.Intent
import com.mutuelle.ragagent.domain.model.Guarantee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service responsible for ingesting documents into the vector store.
 * Handles FAQ, policies, and guarantee documentation.
 */
@Service
class DocumentIngestionService(
    private val vectorStore: VectorStore
) {
    private val textSplitter = TokenTextSplitter.builder()
        .withMinChunkSizeChars(100)
        .withMinChunkLengthToEmbed(50)
        .withMaxNumChunks(100)
        .withKeepSeparator(true)
        .build()

    /**
     * Ingests FAQ documents into the vector store.
     */
    suspend fun ingestFaq(faqDocuments: List<FaqDocument>) {
        logger.info { "Ingesting ${faqDocuments.size} FAQ documents" }

        val documents = faqDocuments.flatMap { faq ->
            val content = """
                Question: ${faq.question}
                Réponse: ${faq.answer}
            """.trimIndent()

            val metadata = mapOf(
                "category" to faq.category.name,
                "subcategory" to (faq.subcategory ?: ""),
                "source" to "faq",
                "question_id" to faq.id,
                "language" to "fr"
            )

            val doc = Document(content, metadata)

            // Chunk long answers
            if (faq.answer.length > 1000) {
                textSplitter.split(listOf(doc))
            } else {
                listOf(doc)
            }
        }

        // Batch insert (run on IO dispatcher for blocking call)
        withContext(Dispatchers.IO) {
            documents.chunked(100).forEachIndexed { index, batch ->
                vectorStore.add(batch)
                logger.info { "Ingested FAQ batch ${index + 1}, total: ${(index + 1) * batch.size}" }
            }
        }

        logger.info { "FAQ ingestion completed: ${documents.size} chunks" }
    }

    /**
     * Ingests guarantee descriptions into the vector store.
     */
    suspend fun ingestGuarantees(guarantees: List<Guarantee>) {
        logger.info { "Ingesting ${guarantees.size} guarantee documents" }

        val documents = guarantees.map { guarantee ->
            val content = """
                Garantie: ${guarantee.name}
                Code: ${guarantee.code}
                Catégorie: ${guarantee.category.displayFr}
                Description: ${guarantee.description}
                Taux de remboursement: ${guarantee.coveragePercentage}%
                Plafond: ${guarantee.ceiling?.let { "$it €" } ?: "Aucun plafond"}
                Fréquence: ${guarantee.frequency?.displayFr ?: "N/A"}
                Délai de carence: ${guarantee.waitingPeriodDays} jours
            """.trimIndent()

            val metadata = mapOf(
                "guarantee_code" to guarantee.code,
                "category" to guarantee.category.name,
                "source" to "guarantee",
                "has_ceiling" to guarantee.hasCeiling.toString()
            )

            Document(content, metadata)
        }

        withContext(Dispatchers.IO) {
            vectorStore.add(documents)
        }
        logger.info { "Guarantee ingestion completed: ${documents.size} documents" }
    }

    /**
     * Ingests policy documents (CGV, notices) into the vector store.
     */
    suspend fun ingestPolicyDocument(
        content: String,
        documentType: String,
        version: String,
        formulaCode: String? = null
    ) {
        logger.info { "Ingesting policy document: $documentType v$version" }

        val metadata = mutableMapOf(
            "document_type" to documentType,
            "version" to version,
            "source" to "policy",
            "language" to "fr"
        )
        formulaCode?.let { metadata["formula_code"] = it }

        val doc = Document(content, metadata.toMap())
        val chunks = textSplitter.split(listOf(doc))

        withContext(Dispatchers.IO) {
            vectorStore.add(chunks)
        }
        logger.info { "Policy document ingested: ${chunks.size} chunks" }
    }

    /**
     * Deletes documents by source type.
     */
    suspend fun deleteBySource(source: String) {
        logger.info { "Deleting documents with source: $source" }
        // Note: Implementation depends on Qdrant client capabilities
        // vectorStore.delete(...) with filter
    }
}

/**
 * Represents a FAQ document to be ingested.
 */
data class FaqDocument(
    val id: String,
    val question: String,
    val answer: String,
    val category: Intent,
    val subcategory: String? = null,
    val keywords: List<String> = emptyList()
)
