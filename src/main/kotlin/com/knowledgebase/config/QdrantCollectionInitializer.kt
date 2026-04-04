package com.knowledgebase.config

import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Collections.OptimizersConfigDiff
import io.qdrant.client.grpc.Collections.UpdateCollection
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class QdrantCollectionInitializer(
    private val qdrantClient: QdrantClient,
    private val qdrantProperties: QdrantProperties,
    private val knowledgeBaseProperties: KnowledgeBaseProperties
) {

    @EventListener(ApplicationReadyEvent::class)
    fun configureCollection() {
        if (!knowledgeBaseProperties.qdrant.configureOnStartup) {
            logger.info { "Qdrant collection startup configuration is disabled" }
            return
        }

        runBlocking {
            try {
                logger.info {
                    "Configuring Qdrant collection: ${qdrantProperties.collectionName} for immediate indexing"
                }

                qdrantClient.updateCollectionAsync(
                    UpdateCollection.newBuilder()
                        .setCollectionName(qdrantProperties.collectionName)
                        .setOptimizersConfig(
                            OptimizersConfigDiff.newBuilder()
                                .setIndexingThreshold(0)
                                .build()
                        )
                        .build()
                ).get()

                logger.info {
                    "Collection ${qdrantProperties.collectionName} configured with indexing_threshold=0"
                }
            } catch (e: Exception) {
                logger.warn(e) { "Could not update collection config (may not exist yet): ${e.message}" }
            }
        }
    }
}
