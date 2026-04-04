package com.mutuelle.ragagent.config

import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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

    @Value("\${spring.ai.vectorstore.qdrant.collection-name:mutuelle-knowledge}")
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
}
