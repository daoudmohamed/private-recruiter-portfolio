package com.knowledgebase.ai.rag

import com.google.common.util.concurrent.Futures
import com.knowledgebase.config.QdrantProperties
import com.knowledgebase.config.RedisKeyspace
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.JsonWithInt
import io.qdrant.client.grpc.Points
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.redis.core.ReactiveHashOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveSetOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class DocumentIngestionServiceTest {

    private lateinit var vectorStore: VectorStore
    private lateinit var redisTemplate: ReactiveRedisTemplate<String, String>
    private lateinit var setOperations: ReactiveSetOperations<String, String>
    private lateinit var hashOperations: ReactiveHashOperations<String, String, String>
    private lateinit var qdrantClient: QdrantClient
    private lateinit var service: DocumentIngestionService

    @BeforeEach
    fun setUp() {
        vectorStore = mockk()
        redisTemplate = mockk()
        setOperations = mockk()
        hashOperations = mockk()
        qdrantClient = mockk()

        every { redisTemplate.opsForSet() } returns setOperations
        every { redisTemplate.opsForHash<String, String>() } returns hashOperations
        every { setOperations.add(any(), *anyVararg()) } returns Mono.just(1L)
        every { setOperations.remove(any(), *anyVararg()) } returns Mono.just(1L)
        every { setOperations.members(any()) } returns Flux.empty()
        every { hashOperations.remove(any(), *anyVararg()) } returns Mono.just(1L)
        every { vectorStore.add(any<List<org.springframework.ai.document.Document>>()) } just runs

        service = DocumentIngestionService(
            vectorStore = vectorStore,
            redisTemplate = redisTemplate,
            qdrantClient = qdrantClient,
            qdrantProperties = QdrantProperties(collectionName = "knowledge-base")
        )
    }

    @Test
    fun `ingestFile should reject unsupported file extension`() = runBlocking {
        val result = service.ingestFile("virus.exe", ByteArrayResource("boom".toByteArray()))

        assertThat(result.success).isFalse()
        assertThat(result.error).contains("Unsupported file type")
        verify(exactly = 0) { vectorStore.add(any<List<org.springframework.ai.document.Document>>()) }
    }

    @Test
    fun `ingestFile should add chunks and register source for supported text file`() = runBlocking {
        val result = service.ingestFile(
            "notes.md",
            ByteArrayResource("# Title\n\nSome knowledge content".toByteArray())
        )

        assertThat(result.success).isTrue()
        assertThat(result.filename).isEqualTo("notes.md")
        assertThat(result.chunksCreated).isGreaterThanOrEqualTo(0)
        verify(exactly = 1) { vectorStore.add(any<List<org.springframework.ai.document.Document>>()) }
        verify(exactly = 1) { setOperations.add(RedisKeyspace.INGESTED_SOURCES_KEY, "notes.md") }
    }

    @Test
    fun `listIngestedSources should return sorted unique sources from qdrant`() = runBlocking {
        every { qdrantClient.scrollAsync(any()) } returnsMany listOf(
            Futures.immediateFuture(
                scrollResponse(
                    retrievedPointWithSource("b.md"),
                    retrievedPointWithSource("a.md"),
                    retrievedPointWithoutSource(),
                    nextPageOffset = 42L
                )
            ),
            Futures.immediateFuture(
                scrollResponse(
                    retrievedPointWithSource("a.md"),
                    retrievedPointWithSource("c.pdf"),
                    retrievedPointWithBlankSource()
                )
            )
        )

        val result = service.listIngestedSources()

        assertThat(result).containsExactly("a.md", "b.md", "c.pdf")
    }

    @Test
    fun `deleteBySource should delete qdrant points and cleanup redis tracking`() = runBlocking {
        every {
            qdrantClient.deleteAsync("knowledge-base", any<Points.Filter>())
        } returns Futures.immediateFuture(mockk(relaxed = true))

        val result = service.deleteBySource("cv.md")

        assertThat(result).isTrue()
        verify(exactly = 1) { setOperations.remove(RedisKeyspace.INGESTED_SOURCES_KEY, "cv.md") }
        verify(exactly = 1) { hashOperations.remove(RedisKeyspace.INGESTED_FILES_KEY, "cv.md") }
    }

    private fun retrievedPointWithSource(source: String): Points.RetrievedPoint {
        return Points.RetrievedPoint.newBuilder()
            .putPayload("source", JsonWithInt.Value.newBuilder().setStringValue(source).build())
            .build()
    }

    private fun retrievedPointWithBlankSource(): Points.RetrievedPoint {
        return Points.RetrievedPoint.newBuilder()
            .putPayload("source", JsonWithInt.Value.newBuilder().setStringValue("   ").build())
            .build()
    }

    private fun retrievedPointWithoutSource(): Points.RetrievedPoint {
        return Points.RetrievedPoint.newBuilder()
            .putPayload("ingested_at", JsonWithInt.Value.newBuilder().setStringValue("now").build())
            .build()
    }

    private fun scrollResponse(
        vararg points: Points.RetrievedPoint,
        nextPageOffset: Long? = null
    ): Points.ScrollResponse {
        return Points.ScrollResponse.newBuilder()
            .addAllResult(points.asList())
            .apply {
                nextPageOffset?.let {
                    setNextPageOffset(Points.PointId.newBuilder().setNum(it).build())
                }
            }
            .build()
    }
}
