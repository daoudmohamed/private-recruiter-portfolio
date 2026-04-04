package com.knowledgebase.application.service

import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.ai.prompt.SystemPromptProvider
import com.knowledgebase.ai.rag.SimpleRetriever
import com.knowledgebase.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Main service for handling chat interactions.
 */
@Service
class ChatService(
    private val chatClient: ChatClient,
    private val sessionService: SessionService,
    private val simpleRetriever: SimpleRetriever,
    private val chatMemory: RedisChatMemory,
    private val systemPromptProvider: SystemPromptProvider
) {
    /**
     * Processes a chat message and returns a streaming response.
     */
    fun streamChat(request: ChatRequest): Flow<ChatChunk> = flow {
        val startTime = System.currentTimeMillis()

        try {
            logger.info { "Processing chat request for session: ${request.sessionId}" }

            // 1. Perform RAG retrieval
            val retrievalResult = withContext(Dispatchers.IO) {
                simpleRetriever.retrieve(request.message)
            }

            logger.debug { "Retrieved ${retrievalResult.documents.size} documents" }

            // 2. Build system prompt with RAG context
            val systemPrompt = systemPromptProvider.getPrompt(
                ragContext = retrievalResult.toContextString()
            )

            // 3. Stream response from LLM
            var responseContent = StringBuilder()

            withContext(Dispatchers.IO) {
                chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.message)
                    .advisors { spec ->
                        spec.param("chat_memory_conversation_id", request.sessionId)
                    }
                    .stream()
                    .content()
                    .asFlow()
            }.collect { chunk ->
                responseContent.append(chunk)
                emit(ChatChunk.content(chunk))
            }

            // 4. Save to memory
            chatMemory.addAsync(
                request.sessionId,
                listOf(
                    UserMessage(request.message),
                    org.springframework.ai.chat.messages.AssistantMessage(responseContent.toString())
                )
            )

            // 5. Update session
            sessionService.updateActivity(request.sessionId)

            // 6. Emit done signal
            emit(ChatChunk.done())

            val duration = System.currentTimeMillis() - startTime
            logger.info { "Chat response completed in ${duration}ms for session: ${request.sessionId}" }

        } catch (e: Exception) {
            logger.error(e) { "Error processing chat request" }
            emit(ChatChunk.error("Désolé, une erreur s'est produite. Veuillez réessayer."))
            emit(ChatChunk.done())
        }
    }

    /**
     * Processes a chat message synchronously (non-streaming).
     */
    suspend fun chat(request: ChatRequest): ChatResponse {
        val chunks = streamChat(request).toList()
        val content = chunks
            .filter { it.type == ChunkType.CONTENT }
            .joinToString("") { it.content }

        return ChatResponse(
            sessionId = request.sessionId,
            content = content
        )
    }
}
