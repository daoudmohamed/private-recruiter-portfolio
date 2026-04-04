package com.knowledgebase.api.controller

import com.knowledgebase.ai.rag.DocumentIngestionService
import com.knowledgebase.ai.rag.FolderScanSummary
import com.knowledgebase.ai.rag.FolderScannerService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DocumentIngestionControllerTest {

    private val documentIngestionService: DocumentIngestionService = mockk()
    private val folderScannerService: FolderScannerService = mockk()
    private val controller = DocumentIngestionController(documentIngestionService, folderScannerService)

    @Test
    fun `scanFolder should expose ingestion summary`() = runBlocking {
        coEvery { folderScannerService.scanFolder() } returns FolderScanSummary(
            totalDocuments = 5,
            documentsIngested = listOf("a.md", "b.md"),
            documentsSkipped = 2,
            failedDocuments = listOf("c.pdf")
        )

        val response = controller.scanFolder()

        assertThat(response.totalDocuments).isEqualTo(5)
        assertThat(response.documentsIngested).containsExactly("a.md", "b.md")
        assertThat(response.status).contains("2 ingested")
        assertThat(response.status).contains("2 skipped")
        assertThat(response.status).contains("1 failed")
    }

    @Test
    fun `listDocuments should return sources and count`() = runBlocking {
        coEvery { documentIngestionService.listIngestedSources() } returns listOf("cv.md", "faq.pdf")

        val response = controller.listDocuments()

        assertThat(response["count"]).isEqualTo(2)
        assertThat(response["sources"]).isEqualTo(listOf("cv.md", "faq.pdf"))
    }

    @Test
    fun `deleteBySource should expose deletion outcome`() = runBlocking {
        coEvery { documentIngestionService.deleteBySource("cv.md") } returns true

        val response = controller.deleteBySource("cv.md")

        assertThat(response["source"]).isEqualTo("cv.md")
        assertThat(response["deleted"]).isEqualTo(true)
        assertThat(response["message"]).isEqualTo("Documents deleted")
    }
}
