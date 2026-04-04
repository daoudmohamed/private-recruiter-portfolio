package com.mutuelle.ragagent.domain.intent

/**
 * Detected intent from a user message.
 */
enum class Intent(val displayFr: String, val requiresAdherentData: Boolean) {
    CONTRAT("Contrat et garanties", true),
    REMBOURSEMENT("Remboursement", true),
    DEVIS("Devis et simulation", true),
    ADMINISTRATIF("Administratif", true),
    RECLAMATION("Réclamation", true),
    ESCALATION("Escalade vers conseiller", true),
    GENERAL("Question générale", false),
    GREETING("Salutation", false),
    UNKNOWN("Inconnu", false);

    companion object {
        fun fromString(value: String): Intent {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * Result of intent classification.
 */
data class IntentClassification(
    val intent: Intent,
    val confidence: Float,
    val subIntents: List<SubIntent> = emptyList(),
    val entities: Map<String, String> = emptyMap(),
    val rawResponse: String? = null
) {
    val isConfident: Boolean
        get() = confidence >= 0.7f

    val isLowConfidence: Boolean
        get() = confidence < 0.3f

    val shouldEscalate: Boolean
        get() = intent == Intent.ESCALATION || isLowConfidence

    companion object {
        fun unknown() = IntentClassification(
            intent = Intent.UNKNOWN,
            confidence = 0f
        )

        fun greeting() = IntentClassification(
            intent = Intent.GREETING,
            confidence = 1f
        )
    }
}

/**
 * Sub-intent for more granular classification.
 */
data class SubIntent(
    val name: String,
    val confidence: Float
)

/**
 * Entity extracted from a user message.
 */
data class ExtractedEntity(
    val type: EntityType,
    val value: String,
    val normalizedValue: String? = null,
    val confidence: Float
)

/**
 * Types of entities that can be extracted.
 */
enum class EntityType {
    DATE,
    AMOUNT,
    CARE_TYPE,
    DOCUMENT_TYPE,
    BENEFICIARY_NAME,
    PROVIDER_NAME,
    CONTRACT_NUMBER,
    REIMBURSEMENT_ID
}

/**
 * Prompt template for intent detection.
 */
object IntentPrompts {
    val CLASSIFICATION_PROMPT = """
        Analyse le message suivant et détermine l'intention principale parmi:
        - CONTRAT: Questions sur les contrats, garanties, souscription, bénéficiaires
        - REMBOURSEMENT: Suivi de remboursements, relevé de prestations, calcul
        - DEVIS: Demande de devis optique, dentaire, hospitalisation, simulation
        - ADMINISTRATIF: Changement d'adresse, attestation, carte mutuelle, RIB
        - RECLAMATION: Plainte, insatisfaction, litige
        - ESCALATION: Demande explicite de parler à un conseiller humain
        - GENERAL: Questions générales sur la mutuelle, FAQ
        - GREETING: Simple salutation (bonjour, salut, etc.)

        Message: {message}

        Réponds en JSON strict:
        {
            "intent": "NOM_INTENT",
            "confidence": 0.0 à 1.0,
            "entities": {"type": "valeur"},
            "reasoning": "explication courte"
        }
    """.trimIndent()
}
