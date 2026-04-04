package com.mutuelle.ragagent.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

/**
 * Configuration for Spring AI components.
 * Sets up ChatClient with advisors for RAG and memory.
 */
@Configuration
class AiConfig {

    @Value("classpath:prompts/system-prompt.st")
    private lateinit var systemPromptResource: Resource

    @Value("\${mutuelle.rag.similarity-threshold:0.65}")
    private var similarityThreshold: Double = 0.65

    @Value("\${mutuelle.rag.top-k:5}")
    private var topK: Int = 5

    /**
     * Creates the main ChatClient with memory advisor.
     * RAG retrieval is handled separately by the ChatService.
     */
    @Bean
    fun chatClient(
        @Qualifier("openAiChatModel") chatModel: ChatModel,
        vectorStore: VectorStore,
        chatMemory: ChatMemory
    ): ChatClient {
        val systemPrompt = systemPromptResource.inputStream.bufferedReader().readText()

        return ChatClient.builder(chatModel)
            .defaultSystem(systemPrompt)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .build()
            )
            .build()
    }

    /**
     * Creates a lightweight ChatClient without RAG for quick operations like intent detection.
     */
    @Bean
    fun lightweightChatClient(@Qualifier("openAiChatModel") chatModel: ChatModel): ChatClient {
        return ChatClient.builder(chatModel)
            .build()
    }

    @Bean
    fun vectorStoreConfig(): VectorStoreConfig {
        return VectorStoreConfig(topK, similarityThreshold)
    }
}

data class VectorStoreConfig(
    val topK: Int,
    val similarityThreshold: Double
)
