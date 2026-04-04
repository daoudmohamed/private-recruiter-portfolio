package com.mutuelle.ragagent.ai.intent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mutuelle.ragagent.domain.intent.Intent
import com.mutuelle.ragagent.domain.intent.IntentClassification
import com.mutuelle.ragagent.domain.intent.IntentPrompts
import mu.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Detects user intent from chat messages using Claude.
 */
@Component
class IntentDetector(
    @Qualifier("lightweightChatClient")
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
    @Value("\${mutuelle.escalation.confidence-threshold:0.3}")
    private val confidenceThreshold: Float
) {
    /**
     * Classifies the intent of a user message.
     */
    suspend fun classify(message: String): IntentClassification {
        logger.debug { "Classifying intent for message: '$message'" }

        // Quick check for greetings
        if (isGreeting(message)) {
            return IntentClassification.greeting()
        }

        // Quick check for escalation keywords
        if (containsEscalationKeywords(message)) {
            return IntentClassification(
                intent = Intent.ESCALATION,
                confidence = 0.95f
            )
        }

        return try {
            val prompt = IntentPrompts.CLASSIFICATION_PROMPT.replace("{message}", message)

            val response = chatClient.prompt()
                .user(prompt)
                .call()
                .content()

            parseIntentResponse(response, message)
        } catch (e: Exception) {
            logger.error(e) { "Failed to classify intent" }
            IntentClassification.unknown()
        }
    }

    /**
     * Parses the JSON response from Claude into an IntentClassification.
     */
    private fun parseIntentResponse(response: String?, message: String): IntentClassification {
        if (response.isNullOrBlank()) {
            return IntentClassification.unknown()
        }

        return try {
            // Extract JSON from response (may be wrapped in markdown code blocks)
            val jsonString = extractJson(response)
            val jsonNode = objectMapper.readTree(jsonString)

            val intentName = jsonNode.get("intent")?.asText() ?: "UNKNOWN"
            val confidence = jsonNode.get("confidence")?.floatValue() ?: 0f
            val entities = jsonNode.get("entities")?.let {
                objectMapper.readValue<Map<String, String>>(it.toString())
            } ?: emptyMap()

            IntentClassification(
                intent = Intent.fromString(intentName),
                confidence = confidence,
                entities = entities,
                rawResponse = response
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse intent response: $response" }
            fallbackClassification(message)
        }
    }

    /**
     * Extracts JSON from a response that may contain markdown code blocks.
     */
    private fun extractJson(response: String): String {
        val jsonPattern = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val match = jsonPattern.find(response)
        return match?.groupValues?.get(1)?.trim() ?: response.trim()
    }

    /**
     * Fallback classification using keyword matching.
     */
    private fun fallbackClassification(message: String): IntentClassification {
        val lowercaseMessage = message.lowercase()

        val intent = when {
            containsContractKeywords(lowercaseMessage) -> Intent.CONTRAT
            containsReimbursementKeywords(lowercaseMessage) -> Intent.REMBOURSEMENT
            containsDevisKeywords(lowercaseMessage) -> Intent.DEVIS
            containsAdminKeywords(lowercaseMessage) -> Intent.ADMINISTRATIF
            containsReclamationKeywords(lowercaseMessage) -> Intent.RECLAMATION
            else -> Intent.GENERAL
        }

        return IntentClassification(
            intent = intent,
            confidence = 0.5f // Lower confidence for fallback
        )
    }

    /**
     * Checks if the message is a simple greeting.
     */
    private fun isGreeting(message: String): Boolean {
        val greetings = listOf(
            "bonjour", "salut", "hello", "hi", "coucou",
            "bonsoir", "hey", "yo"
        )
        val cleaned = message.lowercase().trim()
        return greetings.any { cleaned == it || cleaned.startsWith("$it ") || cleaned.startsWith("$it,") }
    }

    /**
     * Checks if the message contains escalation keywords.
     */
    private fun containsEscalationKeywords(message: String): Boolean {
        val keywords = listOf(
            "parler à un conseiller",
            "parler a un conseiller",
            "conseiller humain",
            "agent humain",
            "personne réelle",
            "personne reelle",
            "quelqu'un",
            "quelqu un"
        )
        val lowercaseMessage = message.lowercase()
        return keywords.any { lowercaseMessage.contains(it) }
    }

    private fun containsContractKeywords(message: String): Boolean {
        val keywords = listOf(
            "contrat", "garantie", "garanties", "couverture",
            "formule", "souscription", "bénéficiaire", "beneficiaire",
            "cotisation", "adhésion", "adhesion"
        )
        return keywords.any { message.contains(it) }
    }

    private fun containsReimbursementKeywords(message: String): Boolean {
        val keywords = listOf(
            "remboursement", "rembourse", "remboursé",
            "prise en charge", "relevé", "releve",
            "prestation", "décompte", "decompte", "virement"
        )
        return keywords.any { message.contains(it) }
    }

    private fun containsDevisKeywords(message: String): Boolean {
        val keywords = listOf(
            "devis", "estimation", "simuler", "simulation",
            "reste à charge", "reste a charge", "combien"
        )
        return keywords.any { message.contains(it) }
    }

    private fun containsAdminKeywords(message: String): Boolean {
        val keywords = listOf(
            "attestation", "carte", "rib", "adresse",
            "modifier", "changer", "télécharger", "telecharger",
            "document"
        )
        return keywords.any { message.contains(it) }
    }

    private fun containsReclamationKeywords(message: String): Boolean {
        val keywords = listOf(
            "réclamation", "reclamation", "plainte",
            "problème", "probleme", "erreur",
            "mécontent", "mecontent", "insatisfait"
        )
        return keywords.any { message.contains(it) }
    }
}
