package com.knowledgebase.ai.rag

import com.knowledgebase.config.KnowledgeBaseProperties
import com.knowledgebase.config.RedisKeyspace
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.FileSystemResource
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.toList

private val logger = KotlinLogging.logger {}

/**
 * Service that scans a local folder for documents on demand.
 * Skips files that have already been ingested (tracked in Redis by fingerprint).
 */
@Component
class FolderScannerService(
    private val documentIngestionService: DocumentIngestionService,
    @param:Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val knowledgeBaseProperties: KnowledgeBaseProperties
) {
    private val supportedExtensions = setOf("pdf", "txt", "md", "markdown")
    private val documentsFolder = knowledgeBaseProperties.documents.folder

    suspend fun scanFolder(): FolderScanSummary {
        logger.info { "Scanning documents folder: $documentsFolder" }

        val path = Path.of(documentsFolder)

        if (!Files.exists(path)) {
            logger.warn { "Documents folder does not exist: $documentsFolder. Creating it..." }
            Files.createDirectories(path)
            logger.info { "Created documents folder: $documentsFolder" }
            return FolderScanSummary()
        }

        if (!path.isDirectory()) {
            throw IllegalStateException("Documents path is not a directory: $documentsFolder")
        }

        val files = Files.walk(path)
            .filter { it.isRegularFile() }
            .filter { it.extension.lowercase() in supportedExtensions }
            .toList()

        if (files.isEmpty()) {
            logger.warn { "No supported files found in folder: $documentsFolder" }
            return FolderScanSummary()
        }

        logger.info { "Found ${files.size} file(s) to check" }

        val results = mutableListOf<IngestResult>()
        var skippedCount = 0

        for (file in files) {
            val fingerprint = computeFingerprint(file)
            val stored = redisTemplate.opsForHash<String, String>()
                .get(RedisKeyspace.INGESTED_FILES_KEY, file.name)
                .awaitSingleOrNull()

            if (stored == fingerprint) {
                skippedCount++
                logger.info { "Skipping already ingested file: ${file.name}" }
                continue
            }

            logger.info { "Ingesting new/modified file: ${file.name}" }
            val result = documentIngestionService.ingestFile(file.name, FileSystemResource(file))
            results.add(result)

            if (result.success) {
                redisTemplate.opsForHash<String, String>()
                    .put(RedisKeyspace.INGESTED_FILES_KEY, file.name, fingerprint)
                    .awaitSingle()
            }
        }

        val successCount = results.count { it.success }
        val failCount = results.count { !it.success }

        logger.info {
            "Folder scan completed: $successCount ingested, $skippedCount skipped (already up-to-date), $failCount failed"
        }

        results.filter { !it.success }.forEach { result ->
            logger.warn { "Failed to ingest ${result.filename}: ${result.error}" }
        }

        return FolderScanSummary(
            totalDocuments = files.size,
            documentsIngested = results.filter { it.success }.map { it.filename },
            documentsSkipped = skippedCount,
            failedDocuments = results.filter { !it.success }.map { it.filename }
        )
    }

    /**
     * Fingerprint = filename:sizeBytes:lastModifiedMillis
     * Allows detecting both new files and modified files.
     */
    private fun computeFingerprint(file: Path): String {
        val size = Files.size(file)
        val lastModified = Files.getLastModifiedTime(file).toMillis()
        return "${file.name}:$size:$lastModified"
    }
}

data class FolderScanSummary(
    val totalDocuments: Int = 0,
    val documentsIngested: List<String> = emptyList(),
    val documentsSkipped: Int = 0,
    val failedDocuments: List<String> = emptyList()
)
