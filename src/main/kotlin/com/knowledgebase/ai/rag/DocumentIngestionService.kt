package com.knowledgebase.ai.rag

import com.knowledgebase.config.QdrantProperties
import com.knowledgebase.config.RedisKeyspace
import io.qdrant.client.ConditionFactory
import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Points
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.ai.reader.TextReader
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.ai.reader.tika.TikaDocumentReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.toList

private val logger = KotlinLogging.logger {}

/**
 * Service for ingesting documents into the vector store.
 */
@Service
class DocumentIngestionService(
    private val vectorStore: VectorStore,
    @param:Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val qdrantClient: QdrantClient,
    private val qdrantProperties: QdrantProperties
) {
    private val textSplitter = TokenTextSplitter.builder()
        .withMinChunkSizeChars(100)
        .withMinChunkLengthToEmbed(50)
        .withMaxNumChunks(200)
        .withKeepSeparator(true)
        .build()

    private val supportedExtensions = setOf("pdf", "txt", "md", "markdown")

    /**
     * Ingest a single file into the vector store.
     */
    suspend fun ingestFile(filename: String, content: Resource): IngestResult {
        logger.info { "Ingesting file: $filename" }

        try {
            val extension = filename.substringAfterLast('.', "").lowercase()

            if (extension !in supportedExtensions) {
                return IngestResult(
                    filename = filename,
                    chunksCreated = 0,
                    success = false,
                    error = "Unsupported file type: $extension. Supported: ${supportedExtensions.joinToString()}"
                )
            }

            // Read documents based on file type
            val documents = when (extension) {
                "pdf" -> readPdf(content)
                "txt", "md", "markdown" -> readText(content)
                else -> emptyList()
            }

            if (documents.isEmpty()) {
                return IngestResult(
                    filename = filename,
                    chunksCreated = 0,
                    success = false,
                    error = "No content extracted from file"
                )
            }

            // Add metadata to all documents
            val timestamp = Instant.now().toString()
            val enrichedDocuments = documents.map { doc ->
                Document(
                    doc.text ?: "",
                    doc.metadata.filterValues { it != null }.toMutableMap().apply {
                        put("source", filename)
                        put("type", extension)
                        put("ingested_at", timestamp)
                    }
                )
            }

            // Split into chunks
            val chunks = textSplitter.apply(enrichedDocuments)

            // Add to vector store (blocking operation, run on IO dispatcher)
            withContext(Dispatchers.IO) {
                vectorStore.add(chunks)
            }

            redisTemplate.opsForSet()
                .add(RedisKeyspace.INGESTED_SOURCES_KEY, filename)
                .awaitSingle()

            logger.info { "Successfully ingested $filename: ${chunks.size} chunks created" }

            return IngestResult(
                filename = filename,
                chunksCreated = chunks.size,
                success = true
            )

        } catch (e: Exception) {
            logger.error(e) { "Failed to ingest file: $filename" }
            return IngestResult(
                filename = filename,
                chunksCreated = 0,
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Scan a local folder and ingest all supported files.
     */
    suspend fun ingestFolder(folderPath: Path): List<IngestResult> {
        logger.info { "Ingesting folder: $folderPath" }

        val files = Files.walk(folderPath)
            .filter { it.isRegularFile() }
            .filter { it.extension.lowercase() in supportedExtensions }
            .toList()

        if (files.isEmpty()) {
            logger.warn { "No supported files found in folder: $folderPath" }
            return emptyList()
        }

        logger.info { "Found ${files.size} files to ingest" }

        return files.map { file ->
            ingestFile(file.name, FileSystemResource(file))
        }
    }

    /**
     * Delete all documents from a given source file.
     */
    suspend fun deleteBySource(filename: String): Boolean {
        logger.info { "Deleting documents from source: $filename" }

        return try {
            val sourceFilter = Points.Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword("source", filename))
                .build()

            withContext(Dispatchers.IO) {
                qdrantClient.deleteAsync(qdrantProperties.collectionName, sourceFilter).get()
            }

            redisTemplate.opsForSet()
                .remove(RedisKeyspace.INGESTED_SOURCES_KEY, filename)
                .awaitSingle()

            redisTemplate.opsForHash<String, String>()
                .remove(RedisKeyspace.INGESTED_FILES_KEY, filename)
                .awaitSingle()

            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete documents from source: $filename" }
            false
        }
    }

    /**
     * List all ingested sources (not implemented - would require vector store scan).
     */
    suspend fun listIngestedSources(): List<String> {
        return try {
            redisTemplate.opsForSet()
                .members(RedisKeyspace.INGESTED_SOURCES_KEY)
                .collectList()
                .awaitSingle()
                .sorted()
        } catch (e: Exception) {
            logger.error(e) { "Failed to list ingested sources" }
            emptyList()
        }
    }

    private fun readPdf(resource: Resource): List<Document> {
        return try {
            val reader = PagePdfDocumentReader(resource)
            reader.get()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read PDF with PagePdfDocumentReader, trying TikaDocumentReader" }
            try {
                val reader = TikaDocumentReader(resource)
                reader.get()
            } catch (e2: Exception) {
                logger.error(e2) { "Failed to read PDF with both readers" }
                emptyList()
            }
        }
    }

    private fun readText(resource: Resource): List<Document> {
        return try {
            val reader = TextReader(resource)
            reader.get()
        } catch (e: Exception) {
            logger.error(e) { "Failed to read text file" }
            emptyList()
        }
    }
}

/**
 * Result of a document ingestion operation.
 */
data class IngestResult(
    val filename: String,
    val chunksCreated: Int,
    val success: Boolean,
    val error: String? = null
)
