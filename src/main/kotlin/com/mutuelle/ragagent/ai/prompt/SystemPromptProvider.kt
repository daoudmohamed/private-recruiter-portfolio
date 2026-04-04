package com.mutuelle.ragagent.ai.prompt

import com.mutuelle.ragagent.ai.context.AdherentContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

/**
 * Provides system prompts for the chat client.
 */
@Component
class SystemPromptProvider(
    @Value("classpath:prompts/system-prompt.st")
    private val systemPromptResource: Resource,
    @Value("\${mutuelle.name:Ma Mutuelle}")
    private val mutuelleName: String
) {
    private lateinit var basePrompt: String

    @PostConstruct
    fun init() {
        basePrompt = systemPromptResource.inputStream.bufferedReader().readText()
    }

    /**
     * Gets the system prompt with context variables replaced.
     */
    fun getPrompt(
        adherentContext: AdherentContext? = null,
        ragContext: String? = null
    ): String {
        var prompt = basePrompt
            .replace("{mutuelle_name}", mutuelleName)

        // Replace adherent context
        val adherentContextString = adherentContext?.toContextString()
            ?: "Aucun contexte adhérent disponible. L'utilisateur n'est peut-être pas authentifié."
        prompt = prompt.replace("{adherent_context}", adherentContextString)

        // Replace RAG context
        val ragContextString = ragContext
            ?: "Aucun document de référence disponible pour cette requête."
        prompt = prompt.replace("{rag_context}", ragContextString)

        return prompt
    }

    /**
     * Gets a minimal prompt for lightweight operations.
     */
    fun getMinimalPrompt(): String {
        return """
            Tu es l'assistant virtuel de $mutuelleName.
            Réponds de manière concise et professionnelle en français.
            Utilise le vouvoiement.
        """.trimIndent()
    }

    companion object {
        /**
         * Template for intent detection prompt.
         */
        val INTENT_DETECTION_TEMPLATE = """
            Analyse le message suivant et détermine l'intention principale.

            Catégories possibles:
            - CONTRAT: Questions sur contrats, garanties, souscription
            - REMBOURSEMENT: Suivi de remboursements, relevés
            - DEVIS: Demandes de devis, simulations
            - ADMINISTRATIF: Documents, changements d'informations
            - RECLAMATION: Plaintes, problèmes
            - ESCALATION: Demande de parler à un humain
            - GENERAL: Questions générales
            - GREETING: Salutations simples

            Message: {message}

            Réponds en JSON: {"intent": "...", "confidence": 0.0-1.0}
        """.trimIndent()

        /**
         * Template for summarizing a conversation for escalation.
         */
        val ESCALATION_SUMMARY_TEMPLATE = """
            Résume brièvement cette conversation pour un conseiller humain.
            Inclus:
            - Le problème principal de l'adhérent
            - Les informations déjà collectées
            - Ce qui a été tenté/proposé

            Conversation:
            {conversation}

            Résumé (max 200 mots):
        """.trimIndent()
    }
}
