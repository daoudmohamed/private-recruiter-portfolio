package com.knowledgebase.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.model.ChatModel
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

    /**
     * Creates the main ChatClient with memory advisor.
     * RAG retrieval is handled separately by the ChatService.
     */
    @Bean
    fun chatClient(
        @Qualifier("openAiChatModel")
        chatModel: ChatModel,
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
    fun lightweightChatClient(
        @Qualifier("openAiChatModel")
        chatModel: ChatModel
    ): ChatClient {
        return ChatClient.builder(chatModel)
            .build()
    }

    @Bean
    fun vectorStoreConfig(knowledgeBaseProperties: KnowledgeBaseProperties): VectorStoreConfig {
        return VectorStoreConfig(
            topK = knowledgeBaseProperties.rag.topK,
            similarityThreshold = knowledgeBaseProperties.rag.similarityThreshold
        )
    }
}

data class VectorStoreConfig(
    val topK: Int,
    val similarityThreshold: Double
)
