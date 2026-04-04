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
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

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
    private val clarificationMessage =
        "Je n'ai pas assez d'elements pour repondre precisement. Posez une question plus claire sur son experience, ses competences techniques ou ses certifications."

    private val vagueMessages = setOf(
        "?", "ok", "oui", "non", "salut", "bonjour", "hello", "hey", "test", "info", "infos", "cv", "profil"
    )

    /**
     * Processes a chat message and returns a streaming response.
     */
    fun streamChat(request: ChatRequest): Flow<ChatChunk> = flow {
        val startTime = System.currentTimeMillis()

        logger.info { "Processing chat request for session: ${request.sessionId}" }

        if (request.message.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be blank")
        }

        if (!sessionService.isSessionActive(request.sessionId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or inactive")
        }

        if (isTooVague(request.message) && !hasUsefulConversationContext(request.sessionId)) {
            emit(
                ChatChunk.content(
                    clarificationMessage
                )
            )
            emit(ChatChunk.done())
            return@flow
        }

        try {
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

            // 4. Update session
            sessionService.updateActivity(request.sessionId)

            // 5. Emit done signal
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

    private fun isTooVague(message: String): Boolean {
        val normalized = message.trim().lowercase()
        if (normalized.length < 4) {
            return true
        }

        if (normalized in vagueMessages) {
            return true
        }

        val wordCount = normalized.split(Regex("\\s+")).count { it.isNotBlank() }
        return wordCount <= 2 && normalized.length < 12
    }

    private fun hasUsefulConversationContext(sessionId: String): Boolean {
        return try {
            val recentMessages = chatMemory.get(sessionId, 4)
            val hasUserMessage = recentMessages.any { it.messageType == MessageType.USER && !it.text.isNullOrBlank() }
            val hasMeaningfulAssistantMessage = recentMessages.any {
                it.messageType == MessageType.ASSISTANT &&
                    !it.text.isNullOrBlank() &&
                    it.text != clarificationMessage
            }

            hasUserMessage && hasMeaningfulAssistantMessage
        } catch (e: Exception) {
            logger.warn(e) { "Unable to inspect chat memory for session: $sessionId" }
            false
        }
    }
}
