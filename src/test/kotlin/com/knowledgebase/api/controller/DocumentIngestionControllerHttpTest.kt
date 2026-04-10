package com.knowledgebase.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.knowledgebase.ai.rag.DocumentIngestionService
import com.knowledgebase.ai.rag.FolderScanSummary
import com.knowledgebase.ai.rag.FolderScannerService
import io.mockk.coEvery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "knowledgebase.security.admin-api-key=test-admin-key",
        "knowledgebase.recruiter-access.enabled=true",
        "knowledgebase.recruiter-access.frontend-base-url=http://localhost:5173",
        "knowledgebase.recruiter-access.token-secret=test-token-secret"
    ]
)
@ActiveProfiles("test")
class DocumentIngestionControllerHttpTest {

    @LocalServerPort
    private var port: Int = 0

    @MockkBean
    private lateinit var documentIngestionService: DocumentIngestionService

    @MockkBean(relaxed = true)
    private lateinit var folderScannerService: FolderScannerService

    @MockkBean
    private lateinit var vectorStore: VectorStore

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `post documents scan should expose scan summary`() {
        coEvery { folderScannerService.scanFolder() } returns FolderScanSummary(
            totalDocuments = 4,
            documentsIngested = listOf("cv.md", "faq.pdf"),
            documentsSkipped = 1,
            failedDocuments = listOf("broken.pdf")
        )

        webTestClient.post()
            .uri("/api/v1/documents/scan")
            .header("X-API-Key", "test-admin-key")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.totalDocuments").isEqualTo(4)
            .jsonPath("$.documentsIngested[0]").isEqualTo("cv.md")
            .jsonPath("$.documentsIngested[1]").isEqualTo("faq.pdf")
            .jsonPath("$.status").isEqualTo("Scan completed: 2 ingested, 1 skipped, 1 failed")
    }

    @Test
    fun `get documents should return listed sources`() {
        coEvery { documentIngestionService.listIngestedSources() } returns listOf("a.md", "b.pdf")

        webTestClient.get()
            .uri("/api/v1/documents")
            .header("X-API-Key", "test-admin-key")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.count").isEqualTo(2)
            .jsonPath("$.sources[0]").isEqualTo("a.md")
            .jsonPath("$.sources[1]").isEqualTo("b.pdf")
    }

    @Test
    fun `delete document source should expose deletion outcome`() {
        coEvery { documentIngestionService.deleteBySource("cv.md") } returns true

        webTestClient.delete()
            .uri("/api/v1/documents/cv.md")
            .header("X-API-Key", "test-admin-key")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.source").isEqualTo("cv.md")
            .jsonPath("$.deleted").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("Documents deleted")
    }

    @Test
    fun `get documents should reject missing admin api key`() {
        webTestClient.get()
            .uri("/api/v1/documents")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
