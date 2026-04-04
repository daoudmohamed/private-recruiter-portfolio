package com.knowledgebase.ai.rag

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.PathResource
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

private const val INGESTED_FILES_KEY = "kb:ingested:files"

/**
 * Service that scans a local folder for documents at startup.
 * Skips files that have already been ingested (tracked in Redis by fingerprint).
 */
@Component
class FolderScannerService(
    private val documentIngestionService: DocumentIngestionService,
    @Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    @Value("\${knowledgebase.documents.folder:documents/}")
    private val documentsFolder: String,
    @Value("\${knowledgebase.documents.scan-on-startup:true}")
    private val scanOnStartup: Boolean
) {
    private val supportedExtensions = setOf("pdf", "txt", "md", "markdown")

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        if (!scanOnStartup) {
            logger.info { "Document folder scanning is disabled" }
            return
        }

        logger.info { "Scanning documents folder: $documentsFolder" }

        val path = Path.of(documentsFolder)

        if (!Files.exists(path)) {
            logger.warn { "Documents folder does not exist: $documentsFolder. Creating it..." }
            try {
                Files.createDirectories(path)
                logger.info { "Created documents folder: $documentsFolder" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to create documents folder" }
            }
            return
        }

        if (!path.isDirectory()) {
            logger.error { "Documents path is not a directory: $documentsFolder" }
            return
        }

        runBlocking {
            try {
                val files = Files.walk(path)
                    .filter { it.isRegularFile() }
                    .filter { it.extension.lowercase() in supportedExtensions }
                    .toList()

                if (files.isEmpty()) {
                    logger.warn { "No supported files found in folder: $documentsFolder" }
                    return@runBlocking
                }

                logger.info { "Found ${files.size} file(s) to check" }

                val results = mutableListOf<IngestResult>()

                for (file in files) {
                    val fingerprint = computeFingerprint(file)
                    val stored = redisTemplate.opsForHash<String, String>()
                        .get(INGESTED_FILES_KEY, file.name)
                        .block()

                    if (stored == fingerprint) {
                        logger.info { "Skipping already ingested file: ${file.name}" }
                        continue
                    }

                    logger.info { "Ingesting new/modified file: ${file.name}" }
                    val result = documentIngestionService.ingestFile(file.name, PathResource(file))
                    results.add(result)

                    if (result.success) {
                        redisTemplate.opsForHash<String, String>()
                            .put(INGESTED_FILES_KEY, file.name, fingerprint)
                            .block()
                    }
                }

                val successCount = results.count { it.success }
                val failCount = results.count { !it.success }
                val skippedCount = files.size - results.size

                logger.info {
                    "Folder scan completed: $successCount ingested, $skippedCount skipped (already up-to-date), $failCount failed"
                }

                results.filter { !it.success }.forEach { result ->
                    logger.warn { "Failed to ingest ${result.filename}: ${result.error}" }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error during folder scanning" }
            }
        }
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
