package com.knowledgebase.api.controller

import com.knowledgebase.ai.rag.DocumentIngestionService
import com.knowledgebase.ai.rag.FolderScannerService
import com.knowledgebase.application.dto.DocumentUploadResponse
import com.knowledgebase.application.dto.IngestionStatusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

private val logger = KotlinLogging.logger {}

/**
 * REST controller for document ingestion.
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Document ingestion endpoints")
class DocumentIngestionController(
    private val documentIngestionService: DocumentIngestionService,
    private val folderScannerService: FolderScannerService
) {
    /**
     * Upload and ingest a document file.
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload a document for ingestion")
    suspend fun uploadDocument(
        @RequestPart("file") filePart: FilePart
    ): DocumentUploadResponse {
        val filename = filePart.filename()
        logger.info { "Uploading document: $filename" }

        try {
            val buffer = DataBufferUtils.join(filePart.content())
                .awaitSingleOrNull()
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file")

            val bytes = try {
                ByteArray(buffer.readableByteCount()).also { buffer.read(it) }
            } finally {
                DataBufferUtils.release(buffer)
            }

            val resource = ByteArrayResource(bytes)
            val result = documentIngestionService.ingestFile(filename, resource)

            return DocumentUploadResponse(
                success = result.success,
                filename = result.filename,
                message = result.error ?: "Document ingested successfully",
                chunksCreated = result.chunksCreated
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to upload document: $filename" }
            return DocumentUploadResponse(
                success = false,
                filename = filename,
                message = e.message ?: "Unknown error",
                chunksCreated = 0
            )
        }
    }

    /**
     * Trigger a manual scan of the documents folder.
     */
    @PostMapping("/scan")
    @Operation(summary = "Trigger a scan of the documents folder")
    suspend fun scanFolder(): IngestionStatusResponse {
        logger.info { "Manual folder scan triggered" }

        val results = folderScannerService.scanFolder()

        return IngestionStatusResponse(
            totalDocuments = results.totalDocuments,
            documentsIngested = results.documentsIngested,
            status = "Scan completed: ${results.documentsIngested.size} ingested, ${results.documentsSkipped} skipped, ${results.failedDocuments.size} failed"
        )
    }

    /**
     * List all ingested document sources.
     */
    @GetMapping
    @Operation(summary = "List all ingested documents")
    suspend fun listDocuments(): Map<String, Any> {
        val sources = documentIngestionService.listIngestedSources()
        return mapOf(
            "sources" to sources,
            "count" to sources.size
        )
    }

    /**
     * Delete documents by source filename.
     */
    @DeleteMapping("/{source:.+}")
    @Operation(summary = "Delete documents by source")
    suspend fun deleteBySource(
        @PathVariable source: String
    ): Map<String, Any> {
        val success = documentIngestionService.deleteBySource(source)
        return mapOf(
            "source" to source,
            "deleted" to success,
            "message" to if (success) "Documents deleted" else "Not implemented or failed"
        )
    }
}
