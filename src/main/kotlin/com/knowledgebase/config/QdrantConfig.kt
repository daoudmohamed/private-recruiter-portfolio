package com.knowledgebase.config

import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.grpc.Collections.OptimizersConfigDiff
import io.qdrant.client.grpc.Collections.UpdateCollection
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

/**
 * Configuration for Qdrant vector store.
 */
@Configuration
class QdrantConfig {

    @Value("\${spring.ai.vectorstore.qdrant.host:localhost}")
    private lateinit var host: String

    @Value("\${spring.ai.vectorstore.qdrant.port:6334}")
    private var port: Int = 6334

    @Value("\${spring.ai.vectorstore.qdrant.api-key:}")
    private var apiKey: String? = null

    @Value("\${spring.ai.vectorstore.qdrant.collection-name:knowledge-base}")
    private lateinit var collectionName: String

    /**
     * Creates a Qdrant client for direct operations.
     */
    @Bean
    fun qdrantClient(): QdrantClient {
        val grpcClientBuilder = QdrantGrpcClient.newBuilder(host, port, false)

        if (!apiKey.isNullOrBlank()) {
            grpcClientBuilder.withApiKey(apiKey.toString())
        }

        return QdrantClient(grpcClientBuilder.build())
    }

    /**
     * Creates a QdrantVectorStore for Spring AI integration.
     */
    @Bean
    fun vectorStore(
        qdrantClient: QdrantClient,
        embeddingModel: EmbeddingModel
    ): QdrantVectorStore {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
            .collectionName(collectionName)
            .initializeSchema(true)
            .build()
    }

    /**
     * Configure collection for immediate indexing after initialization.
     */
    @PostConstruct
    fun configureCollection() {
        runBlocking {
            try {
                logger.info { "Configuring Qdrant collection: $collectionName for immediate indexing" }

                val client = qdrantClient()

                // Update collection to use immediate indexing
                client.updateCollectionAsync(
                    UpdateCollection.newBuilder()
                        .setCollectionName(collectionName)
                        .setOptimizersConfig(
                            OptimizersConfigDiff.newBuilder()
                                .setIndexingThreshold(0)
                                .build()
                        )
                        .build()
                ).get()

                logger.info { "Collection $collectionName configured with indexing_threshold=0" }
            } catch (e: Exception) {
                logger.warn(e) { "Could not update collection config (may not exist yet): ${e.message}" }
            }
        }
    }
}
