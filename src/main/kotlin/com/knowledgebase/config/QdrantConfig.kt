package com.knowledgebase.config

import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Qdrant vector store.
 */
@Configuration
class QdrantConfig(
    private val qdrantProperties: QdrantProperties
) {

    /**
     * Creates a Qdrant client for direct operations.
     */
    @Bean
    fun qdrantClient(): QdrantClient {
        val grpcClientBuilder = QdrantGrpcClient.newBuilder(
            qdrantProperties.host,
            qdrantProperties.port,
            false
        )

        if (qdrantProperties.apiKey.isNotBlank()) {
            grpcClientBuilder.withApiKey(qdrantProperties.apiKey)
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
            .collectionName(qdrantProperties.collectionName)
            .initializeSchema(qdrantProperties.initializeSchema)
            .build()
    }
}
