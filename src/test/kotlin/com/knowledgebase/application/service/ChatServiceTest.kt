package com.knowledgebase.application.service

import com.knowledgebase.ai.memory.RedisChatMemory
import com.knowledgebase.ai.prompt.SystemPromptProvider
import com.knowledgebase.ai.rag.RetrievalResult
import com.knowledgebase.ai.rag.SimpleRetriever
import com.knowledgebase.domain.model.ChatRequest
import com.knowledgebase.domain.model.ChunkType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux

class ChatServiceTest {

    private lateinit var chatClient: ChatClient
    private lateinit var requestSpec: ChatClient.ChatClientRequestSpec
    private lateinit var streamResponseSpec: ChatClient.StreamResponseSpec
    private lateinit var sessionService: SessionService
    private lateinit var simpleRetriever: SimpleRetriever
    private lateinit var chatMemory: RedisChatMemory
    private lateinit var systemPromptProvider: SystemPromptProvider
    private lateinit var chatService: ChatService

    @BeforeEach
    fun setUp() {
        chatClient = mockk()
        requestSpec = mockk()
        streamResponseSpec = mockk()
        sessionService = mockk()
        simpleRetriever = mockk()
        chatMemory = mockk()
        systemPromptProvider = mockk()

        every { chatClient.prompt() } returns requestSpec
        every { requestSpec.system(any<String>()) } returns requestSpec
        every { requestSpec.user(any<String>()) } returns requestSpec
        every { requestSpec.advisors(any<java.util.function.Consumer<ChatClient.AdvisorSpec>>()) } returns requestSpec
        every { requestSpec.stream() } returns streamResponseSpec
        every { streamResponseSpec.content() } returns Flux.just("Bonjour", " !")
        every { systemPromptProvider.getPrompt(any()) } returns "system prompt"
        every { chatMemory.get(any(), any()) } returns mutableListOf()
        coEvery { simpleRetriever.retrieve(any(), any()) } returns RetrievalResult(emptyList())
        coEvery { sessionService.updateActivity(any()) } just runs

        chatService = ChatService(
            chatClient = chatClient,
            sessionService = sessionService,
            simpleRetriever = simpleRetriever,
            chatMemory = chatMemory,
            systemPromptProvider = systemPromptProvider
        )
    }

    @Test
    fun `streamChat should fail with bad request when message is blank`() = runBlocking {
        coEvery { sessionService.isSessionActive(any()) } returns true

        val thrown = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            runBlocking {
                chatService.streamChat(
                    ChatRequest(sessionId = "session-123", message = "   ")
                ).toList()
            }
        }

        assertThat(thrown.statusCode.value()).isEqualTo(400)
    }

    @Test
    fun `streamChat should fail with not found when session is inactive`() = runBlocking {
        coEvery { sessionService.isSessionActive("missing-session") } returns false

        val thrown = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            runBlocking {
                chatService.streamChat(
                    ChatRequest(sessionId = "missing-session", message = "Bonjour")
                ).toList()
            }
        }

        assertThat(thrown.statusCode.value()).isEqualTo(404)
    }

    @Test
    fun `streamChat should emit content chunks and done then update session on success`() = runBlocking {
        coEvery { sessionService.isSessionActive("session-123") } returns true

        val chunks = chatService.streamChat(
            ChatRequest(sessionId = "session-123", message = "Quelle est son experience en microservices ?")
        ).toList()

        assertThat(chunks.map { it.type }).containsExactly(
            ChunkType.CONTENT,
            ChunkType.CONTENT,
            ChunkType.DONE
        )
        assertThat(chunks[0].content).isEqualTo("Bonjour")
        assertThat(chunks[1].content).isEqualTo(" !")

        coVerify(exactly = 1) { simpleRetriever.retrieve("Quelle est son experience en microservices ?", null) }
        coVerify(exactly = 1) { sessionService.updateActivity("session-123") }
    }

    @Test
    fun `streamChat should short circuit vague input without calling retriever or llm`() = runBlocking {
        coEvery { sessionService.isSessionActive("session-123") } returns true

        val chunks = chatService.streamChat(
            ChatRequest(sessionId = "session-123", message = "cv")
        ).toList()

        assertThat(chunks.map { it.type }).containsExactly(
            ChunkType.CONTENT,
            ChunkType.DONE
        )
        assertThat(chunks.first().content).contains("Je n'ai pas assez d'elements")

        coVerify(exactly = 0) { simpleRetriever.retrieve(any(), any()) }
        coVerify(exactly = 0) { sessionService.updateActivity(any()) }
    }

    @Test
    fun `chat should aggregate streamed content`() = runBlocking {
        coEvery { sessionService.isSessionActive("session-123") } returns true

        val response = chatService.chat(ChatRequest(sessionId = "session-123", message = "Quelle est son experience en microservices ?"))

        assertThat(response.sessionId).isEqualTo("session-123")
        assertThat(response.content).isEqualTo("Bonjour !")
    }

    @Test
    fun `chat should return clarification message for vague input`() = runBlocking {
        coEvery { sessionService.isSessionActive("session-123") } returns true

        val response = chatService.chat(ChatRequest(sessionId = "session-123", message = "ok"))

        assertThat(response.sessionId).isEqualTo("session-123")
        assertThat(response.content).contains("Posez une question plus claire")
    }

    @Test
    fun `streamChat should allow vague follow up when conversation context exists`() = runBlocking {
        coEvery { sessionService.isSessionActive("session-123") } returns true
        every { chatMemory.get("session-123", 4) } returns mutableListOf(
            UserMessage("Parle-moi de son experience"),
            AssistantMessage("Il a travaille sur des architectures microservices en environnement bancaire.")
        )

        val chunks = chatService.streamChat(
            ChatRequest(sessionId = "session-123", message = "ok")
        ).toList()

        assertThat(chunks.map { it.type }).containsExactly(
            ChunkType.CONTENT,
            ChunkType.CONTENT,
            ChunkType.DONE
        )
        coVerify(exactly = 1) { simpleRetriever.retrieve("ok", null) }
        coVerify(exactly = 1) { sessionService.updateActivity("session-123") }
    }

    @Test
    fun `streamChat should keep blocking vague follow up after clarification only`() = runBlocking {
        coEvery { sessionService.isSessionActive("session-123") } returns true
        every { chatMemory.get("session-123", 4) } returns mutableListOf(
            UserMessage("cv"),
            AssistantMessage("Je n'ai pas assez d'elements pour repondre precisement. Posez une question plus claire sur son experience, ses competences techniques ou ses certifications.")
        )

        val chunks = chatService.streamChat(
            ChatRequest(sessionId = "session-123", message = "oui")
        ).toList()

        assertThat(chunks.map { it.type }).containsExactly(
            ChunkType.CONTENT,
            ChunkType.DONE
        )
        assertThat(chunks.first().content).contains("Je n'ai pas assez d'elements")
        coVerify(exactly = 0) { simpleRetriever.retrieve(any(), any()) }
    }
}
