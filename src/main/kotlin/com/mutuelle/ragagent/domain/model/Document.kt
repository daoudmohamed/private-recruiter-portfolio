package com.mutuelle.ragagent.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Represents a document associated with an adherent.
 */
data class Document(
    val id: UUID,
    val adherentId: UUID,
    val type: DocumentType,
    val title: String,
    val filename: String,
    val contentType: String,
    val filePath: String,
    val fileSize: Long,
    val generatedAt: Instant,
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    val isValid: Boolean
        get() = validUntil?.let { LocalDate.now().isBefore(it) } ?: true

    val isExpired: Boolean
        get() = validUntil?.let { LocalDate.now().isAfter(it) } ?: false
}

/**
 * Type of document.
 */
enum class DocumentType(val displayFr: String, val description: String) {
    CARTE_TIERS_PAYANT(
        "Carte tiers payant",
        "Carte permettant le tiers payant chez les professionnels de santé"
    ),
    ATTESTATION_DROITS(
        "Attestation de droits",
        "Attestation prouvant votre affiliation à la mutuelle"
    ),
    RELEVE_PRESTATIONS(
        "Relevé de prestations",
        "Récapitulatif des remboursements sur une période"
    ),
    ECHEANCIER_COTISATIONS(
        "Échéancier de cotisations",
        "Planning des prélèvements de cotisations"
    ),
    CONDITIONS_GENERALES(
        "Conditions générales",
        "Document contractuel détaillant les garanties"
    ),
    TABLEAU_GARANTIES(
        "Tableau des garanties",
        "Synthèse des garanties de votre contrat"
    ),
    DECOMPTE(
        "Décompte de remboursement",
        "Détail d'un remboursement spécifique"
    ),
    FACTURE(
        "Facture",
        "Facture de soins ou de cotisation"
    ),
    DEVIS(
        "Devis",
        "Devis de soins"
    ),
    AUTRE(
        "Autre document",
        "Document divers"
    )
}

/**
 * Request to generate a document.
 */
data class DocumentGenerationRequest(
    val adherentId: UUID,
    val type: DocumentType,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * Response after document generation.
 */
data class DocumentGenerationResponse(
    val documentId: UUID,
    val status: DocumentGenerationStatus,
    val downloadUrl: String? = null,
    val expiresAt: Instant? = null,
    val message: String? = null
)

enum class DocumentGenerationStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}
