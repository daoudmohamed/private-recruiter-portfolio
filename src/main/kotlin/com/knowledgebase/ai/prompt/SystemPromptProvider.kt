package com.knowledgebase.ai.prompt

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

/**
 * Provides system prompts for the chat client.
 */
@Component
class SystemPromptProvider(
    @param:Value("classpath:prompts/system-prompt.st")
    private val systemPromptResource: Resource
) {
    private lateinit var basePrompt: String

    @PostConstruct
    fun init() {
        basePrompt = systemPromptResource.inputStream.bufferedReader().readText()
    }

    /**
     * Gets the system prompt with RAG context.
     */
    fun getPrompt(ragContext: String? = null): String {
        val ragContextString = ragContext
            ?: "Aucun document pertinent trouvé pour cette question."

        return basePrompt.replace("{rag_context}", ragContextString)
    }
}
