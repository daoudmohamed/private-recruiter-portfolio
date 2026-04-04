package com.mutuelle.ragagent.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Represents a reimbursement claim.
 */
data class Reimbursement(
    val id: UUID,
    val adherentId: UUID,
    val contractId: UUID,
    val beneficiaryId: UUID? = null,
    val beneficiaryName: String? = null,
    val careDate: LocalDate,
    val submissionDate: LocalDate,
    val processingDate: LocalDate? = null,
    val paymentDate: LocalDate? = null,
    val status: ReimbursementStatus,
    val careType: CareType,
    val careCode: String? = null,
    val careLabel: String,
    val providerName: String? = null,
    val providerCode: String? = null,
    val amountClaimed: BigDecimal,
    val secuBase: BigDecimal? = null,
    val secuAmount: BigDecimal? = null,
    val mutuelleAmount: BigDecimal? = null,
    val totalReimbursed: BigDecimal? = null,
    val remainingCharge: BigDecimal? = null,
    val rejectionReason: String? = null,
    val documents: List<UUID> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isCompleted: Boolean
        get() = status == ReimbursementStatus.COMPLETED

    val isPending: Boolean
        get() = status in listOf(ReimbursementStatus.SUBMITTED, ReimbursementStatus.PROCESSING)

    val reimbursementRate: BigDecimal?
        get() = if (amountClaimed > BigDecimal.ZERO && totalReimbursed != null) {
            totalReimbursed.divide(amountClaimed, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else null
}

/**
 * Status of a reimbursement claim.
 */
enum class ReimbursementStatus(val displayFr: String, val icon: String) {
    SUBMITTED("Soumis", "📤"),
    PROCESSING("En traitement", "⏳"),
    COMPLETED("Remboursé", "✅"),
    REJECTED("Rejeté", "❌"),
    PENDING_INFO("Information manquante", "⚠️")
}

/**
 * Type of healthcare service.
 */
enum class CareType(val displayFr: String, val category: GuaranteeCategory) {
    // Médecine générale
    CONSULTATION_GENERALISTE("Consultation généraliste", GuaranteeCategory.MEDECINE_GENERALE),
    CONSULTATION_SPECIALISTE("Consultation spécialiste", GuaranteeCategory.MEDECINE_GENERALE),
    VISITE_DOMICILE("Visite à domicile", GuaranteeCategory.MEDECINE_GENERALE),

    // Pharmacie
    MEDICAMENTS("Médicaments", GuaranteeCategory.PHARMACIE),

    // Optique
    LUNETTES("Lunettes", GuaranteeCategory.OPTIQUE),
    LENTILLES("Lentilles", GuaranteeCategory.OPTIQUE),
    MONTURE("Monture", GuaranteeCategory.OPTIQUE),
    VERRES("Verres", GuaranteeCategory.OPTIQUE),

    // Dentaire
    SOINS_DENTAIRES("Soins dentaires", GuaranteeCategory.DENTAIRE),
    PROTHESE_DENTAIRE("Prothèse dentaire", GuaranteeCategory.DENTAIRE),
    ORTHODONTIE("Orthodontie", GuaranteeCategory.DENTAIRE),
    IMPLANT_DENTAIRE("Implant dentaire", GuaranteeCategory.DENTAIRE),

    // Hospitalisation
    HOSPITALISATION("Hospitalisation", GuaranteeCategory.HOSPITALISATION),
    CHIRURGIE("Chirurgie", GuaranteeCategory.HOSPITALISATION),
    CHAMBRE_PARTICULIERE("Chambre particulière", GuaranteeCategory.HOSPITALISATION),

    // Auxiliaires médicaux
    KINESITHERAPIE("Kinésithérapie", GuaranteeCategory.AUXILIAIRES_MEDICAUX),
    ORTHOPHONIE("Orthophonie", GuaranteeCategory.AUXILIAIRES_MEDICAUX),
    INFIRMIER("Soins infirmiers", GuaranteeCategory.AUXILIAIRES_MEDICAUX),

    // Autres
    ANALYSES("Analyses médicales", GuaranteeCategory.MEDECINE_GENERALE),
    RADIOLOGIE("Radiologie", GuaranteeCategory.MEDECINE_GENERALE),
    PREVENTION("Acte de prévention", GuaranteeCategory.PREVENTION),
    AUTRE("Autre", GuaranteeCategory.MEDECINE_GENERALE)
}

/**
 * Summary of reimbursements for a period.
 */
data class ReimbursementSummary(
    val adherentId: UUID,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalClaimed: BigDecimal,
    val totalSecuReimbursed: BigDecimal,
    val totalMutuelleReimbursed: BigDecimal,
    val totalRemainingCharge: BigDecimal,
    val countByStatus: Map<ReimbursementStatus, Int>,
    val countByCategory: Map<GuaranteeCategory, Int>
)

/**
 * Consumption tracking for ceilings (plafonds).
 */
data class ConsumptionTracker(
    val adherentId: UUID,
    val category: GuaranteeCategory,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val ceiling: BigDecimal,
    val consumed: BigDecimal
) {
    val remaining: BigDecimal
        get() = (ceiling - consumed).coerceAtLeast(BigDecimal.ZERO)

    val consumptionPercentage: Int
        get() = if (ceiling > BigDecimal.ZERO) {
            consumed.divide(ceiling, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .toInt()
        } else 0
}
