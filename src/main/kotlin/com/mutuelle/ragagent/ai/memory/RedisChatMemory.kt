package com.mutuelle.ragagent.ai.memory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Redis-based chat memory implementation for storing conversation history.
 */
@Component
class RedisChatMemory(
    @Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${mutuelle.chat.max-history-messages:20}")
    private val maxMessages: Int,
    @Value("\${mutuelle.chat.session-timeout-hours:24}")
    private val sessionTimeoutHours: Long
) : ChatMemory {

    private val keyPrefix = "chat:memory:"
    private val ttl = Duration.ofHours(sessionTimeoutHours)

    override fun add(conversationId: String, messages: MutableList<Message>) {
        runBlocking {
            addAsync(conversationId, messages)
        }
    }

    override fun get(conversationId: String): MutableList<Message> {
        return runBlocking {
            getAsync(conversationId, maxMessages)
        }
    }

    fun get(conversationId: String, lastN: Int): MutableList<Message> {
        return runBlocking {
            getAsync(conversationId, lastN)
        }
    }

    override fun clear(conversationId: String) {
        runBlocking {
            clearAsync(conversationId)
        }
    }

    /**
     * Async version of add for use in coroutines.
     */
    suspend fun addAsync(conversationId: String, messages: List<Message>) {
        val key = "$keyPrefix$conversationId"

        try {
            val serializedMessages = messages.map { message ->
                objectMapper.writeValueAsString(serializeMessage(message))
            }

            redisTemplate.opsForList()
                .rightPushAll(key, serializedMessages)
                .awaitSingle()

            // Set TTL
            redisTemplate.expire(key, ttl).awaitSingle()

            // Trim to max size (keep last N messages)
            val listSize = redisTemplate.opsForList().size(key).awaitSingle()
            if (listSize > maxMessages) {
                redisTemplate.opsForList()
                    .trim(key, listSize - maxMessages, -1)
                    .awaitSingleOrNull()
            }

            logger.debug { "Added ${messages.size} messages to conversation: $conversationId" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to add messages to Redis for conversation: $conversationId" }
        }
    }

    /**
     * Async version of get for use in coroutines.
     */
    suspend fun getAsync(conversationId: String, lastN: Int): MutableList<Message> {
        val key = "$keyPrefix$conversationId"

        return try {
            val serializedMessages = redisTemplate.opsForList()
                .range(key, -lastN.toLong(), -1)
                .collectList()
                .awaitSingle()

            serializedMessages.mapNotNull { serialized ->
                try {
                    deserializeMessage(objectMapper.readValue<SerializedMessage>(serialized))
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to deserialize message" }
                    null
                }
            }.toMutableList()
        } catch (e: Exception) {
            logger.error(e) { "Failed to get messages from Redis for conversation: $conversationId" }
            mutableListOf()
        }
    }

    /**
     * Async version of clear for use in coroutines.
     */
    suspend fun clearAsync(conversationId: String) {
        val key = "$keyPrefix$conversationId"

        try {
            redisTemplate.delete(key).awaitSingle()
            logger.debug { "Cleared conversation: $conversationId" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to clear conversation from Redis: $conversationId" }
        }
    }

    /**
     * Gets the count of messages in a conversation.
     */
    suspend fun getMessageCount(conversationId: String): Long {
        val key = "$keyPrefix$conversationId"
        return try {
            redisTemplate.opsForList().size(key).awaitSingle()
        } catch (e: Exception) {
            0L
        }
    }

    private fun serializeMessage(message: Message): SerializedMessage {
        return SerializedMessage(
            type = message.messageType.name,
            content = message.text ?: ""
        )
    }

    private fun deserializeMessage(serialized: SerializedMessage): Message? {
        return when (MessageType.valueOf(serialized.type)) {
            MessageType.USER -> UserMessage(serialized.content)
            MessageType.ASSISTANT -> AssistantMessage(serialized.content)
            else -> null
        }
    }

    /**
     * Internal class for JSON serialization.
     */
    private data class SerializedMessage(
        val type: String,
        val content: String
    )
}
