package com.mutuelle.ragagent.application.service

import com.mutuelle.ragagent.ai.context.ContextBuilder
import com.mutuelle.ragagent.ai.intent.IntentDetector
import com.mutuelle.ragagent.ai.memory.RedisChatMemory
import com.mutuelle.ragagent.ai.prompt.SystemPromptProvider
import com.mutuelle.ragagent.ai.rag.HybridRetriever
import com.mutuelle.ragagent.domain.event.ChatMessageReceivedEvent
import com.mutuelle.ragagent.domain.event.ChatResponseSentEvent
import com.mutuelle.ragagent.domain.model.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Main service for handling chat interactions.
 */
@Service
class ChatService(
    private val chatClient: ChatClient,
    private val sessionService: SessionService,
    private val contextBuilder: ContextBuilder,
    private val intentDetector: IntentDetector,
    private val hybridRetriever: HybridRetriever,
    private val escalationService: EscalationService,
    private val chatMemory: RedisChatMemory,
    private val systemPromptProvider: SystemPromptProvider,
    private val meterRegistry: MeterRegistry,
    @Value("\${mutuelle.escalation.confidence-threshold:0.3}")
    private val confidenceThreshold: Float
) {
    /**
     * Processes a chat message and returns a streaming response.
     * Uses Dispatchers.IO for blocking operations to avoid blocking reactor threads.
     */
    fun streamChat(
        request: ChatRequest,
        adherentId: UUID
    ): Flow<ChatChunk> = flow {
        val startTime = System.currentTimeMillis()
        val timer = Timer.start(meterRegistry)

        try {
            logger.info { "Processing chat request for session: ${request.sessionId}" }

            // 1. Detect intent (run on IO dispatcher for blocking call)
            val intentClassification = withContext(Dispatchers.IO) {
                intentDetector.classify(request.message)
            }
            logger.debug { "Detected intent: ${intentClassification.intent} (confidence: ${intentClassification.confidence})" }

            // 2. Check for escalation
            if (intentClassification.shouldEscalate) {
                handleEscalation(request, adherentId, intentClassification.intent.name)
                emit(ChatChunk.escalation(
                    "Je comprends que vous souhaitez parler à un conseiller. " +
                    "Je transmets votre demande et un conseiller vous contactera très prochainement."
                ))
                emit(ChatChunk.done())
                return@flow
            }

            // 3. Build adherent context
            val adherentContext = contextBuilder.buildContext(adherentId, intentClassification.intent)

            // 4. Perform RAG retrieval (run on IO dispatcher for blocking call)
            val retrievalResult = withContext(Dispatchers.IO) {
                hybridRetriever.retrieve(
                    query = request.message,
                    adherentContext = adherentContext,
                    intent = intentClassification.intent
                )
            }

            // 5. Build system prompt with context
            val systemPrompt = systemPromptProvider.getPrompt(
                adherentContext = adherentContext,
                ragContext = retrievalResult.toContextString()
            )

            // 6. Stream response from LLM (run on IO dispatcher)
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

            // 7. Save to memory
            chatMemory.addAsync(
                request.sessionId,
                listOf(
                    UserMessage(request.message),
                    org.springframework.ai.chat.messages.AssistantMessage(responseContent.toString())
                )
            )

            // 8. Update session
            sessionService.updateActivity(request.sessionId, intentClassification.intent)

            // 9. Emit done signal
            emit(ChatChunk.done())

            // 10. Record metrics
            val duration = System.currentTimeMillis() - startTime
            timer.stop(Timer.builder("chat.response.time")
                .tag("intent", intentClassification.intent.name)
                .register(meterRegistry))

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
    suspend fun chat(
        request: ChatRequest,
        adherentId: UUID
    ): ChatResponse {
        val chunks = streamChat(request, adherentId).toList()
        val content = chunks
            .filter { it.type == ChunkType.CONTENT }
            .joinToString("") { it.content }

        val escalated = chunks.any { it.type == ChunkType.ESCALATION }

        return ChatResponse(
            sessionId = request.sessionId,
            content = content,
            escalated = escalated
        )
    }

    /**
     * Handles escalation to human agent.
     */
    private suspend fun handleEscalation(
        request: ChatRequest,
        adherentId: UUID,
        reason: String
    ) {
        escalationService.escalate(
            sessionId = request.sessionId,
            adherentId = adherentId,
            lastMessage = request.message,
            reason = "Intent: $reason"
        )
    }
}
