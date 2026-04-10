package com.knowledgebase.ai.rag

import com.knowledgebase.config.KnowledgeBaseProperties
import com.knowledgebase.config.RedisKeyspace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.Resource
import org.springframework.data.redis.core.ReactiveHashOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Path

class FolderScannerServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private val documentIngestionService: DocumentIngestionService = mockk()
    private val redisTemplate: ReactiveRedisTemplate<String, String> = mockk()
    private val hashOperations: ReactiveHashOperations<String, String, String> = mockk()

    @Test
    fun `scanFolder should create missing directory and return empty summary`() = runBlocking {
        every { redisTemplate.opsForHash<String, String>() } returns hashOperations
        val documentsFolder = tempDir.resolve("documents")
        val service = buildService(documentsFolder)

        val summary = service.scanFolder()

        assertThat(Files.exists(documentsFolder)).isTrue()
        assertThat(summary.totalDocuments).isZero()
        assertThat(summary.documentsIngested).isEmpty()
        assertThat(summary.documentsSkipped).isZero()
        assertThat(summary.failedDocuments).isEmpty()
    }

    @Test
    fun `scanFolder should skip already ingested files and ingest new supported files`() = runBlocking {
        every { redisTemplate.opsForHash<String, String>() } returns hashOperations

        val existingFile = tempDir.resolve("existing.md")
        Files.writeString(existingFile, "existing content")
        val newFile = tempDir.resolve("new.txt")
        Files.writeString(newFile, "new content")
        Files.writeString(tempDir.resolve("ignored.jpg"), "binary")

        val existingFingerprint = fingerprintFor(existingFile)

        every { hashOperations.get(RedisKeyspace.INGESTED_FILES_KEY, "existing.md") } returns Mono.just(existingFingerprint)
        every { hashOperations.get(RedisKeyspace.INGESTED_FILES_KEY, "new.txt") } returns Mono.empty()
        every { hashOperations.put(RedisKeyspace.INGESTED_FILES_KEY, "new.txt", fingerprintFor(newFile)) } returns Mono.just(true)

        coEvery { documentIngestionService.ingestFile("new.txt", any<Resource>()) } returns IngestResult(
            filename = "new.txt",
            chunksCreated = 3,
            success = true
        )

        val service = buildService(tempDir)
        val summary = service.scanFolder()

        assertThat(summary.totalDocuments).isEqualTo(2)
        assertThat(summary.documentsSkipped).isEqualTo(1)
        assertThat(summary.documentsIngested).containsExactly("new.txt")
        assertThat(summary.failedDocuments).isEmpty()
        coVerify(exactly = 1) { documentIngestionService.ingestFile("new.txt", any<Resource>()) }
        coVerify(exactly = 0) { documentIngestionService.ingestFile("existing.md", any<Resource>()) }
    }

    private fun buildService(folder: Path): FolderScannerService {
        val properties = KnowledgeBaseProperties(
            documents = KnowledgeBaseProperties.Documents(folder = folder.toString())
        )

        return FolderScannerService(
            documentIngestionService = documentIngestionService,
            redisTemplate = redisTemplate,
            knowledgeBaseProperties = properties
        )
    }

    private fun fingerprintFor(file: Path): String {
        val size = Files.size(file)
        val lastModified = Files.getLastModifiedTime(file).toMillis()
        return "${file.fileName}:$size:$lastModified"
    }
}
