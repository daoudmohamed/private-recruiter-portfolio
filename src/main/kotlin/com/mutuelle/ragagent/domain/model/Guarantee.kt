package com.mutuelle.ragagent.domain.model

import java.math.BigDecimal
import java.util.UUID

/**
 * Represents a guarantee (coverage) within a contract.
 */
data class Guarantee(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String,
    val category: GuaranteeCategory,
    val coveragePercentage: Int,
    val ceiling: BigDecimal? = null,
    val frequency: CoverageFrequency? = null,
    val waitingPeriodDays: Int = 0
) {
    val hasCeiling: Boolean
        get() = ceiling != null

    val coverageDisplay: String
        get() = buildString {
            append("$coveragePercentage%")
            ceiling?.let { append(" (plafond: ${it}€)") }
            frequency?.let { append(" par ${it.displayFr}") }
        }
}

/**
 * Category of healthcare coverage.
 */
enum class GuaranteeCategory(val displayFr: String, val icon: String) {
    HOSPITALISATION("Hospitalisation", "🏥"),
    OPTIQUE("Optique", "👓"),
    DENTAIRE("Dentaire", "🦷"),
    MEDECINE_GENERALE("Médecine générale", "👨‍⚕️"),
    PHARMACIE("Pharmacie", "💊"),
    AUXILIAIRES_MEDICAUX("Auxiliaires médicaux", "🩺"),
    MATERNITE("Maternité", "👶"),
    PREVENTION("Prévention", "💉"),
    BIEN_ETRE("Bien-être", "🧘"),
    APPAREILLAGE("Appareillage", "🦿")
}

/**
 * Frequency of coverage renewal.
 */
enum class CoverageFrequency(val displayFr: String, val months: Int) {
    ANNUAL("an", 12),
    BIENNIAL("2 ans", 24),
    TRIENNIAL("3 ans", 36)
}

/**
 * Detailed optical guarantee structure.
 */
data class OpticalGuarantee(
    val monture: BigDecimal,
    val verresSimples: BigDecimal,
    val verresComplexes: BigDecimal,
    val verresTresComplexes: BigDecimal,
    val lentilles: BigDecimal,
    val frequency: CoverageFrequency = CoverageFrequency.BIENNIAL
)

/**
 * Detailed dental guarantee structure.
 */
data class DentalGuarantee(
    val soinsConservatoires: Int, // Percentage
    val protheses: Int, // Percentage
    val orthodontie: BigDecimal?, // Annual ceiling
    val implants: BigDecimal? // Per implant
)

/**
 * Detailed hospitalization guarantee structure.
 */
data class HospitalizationGuarantee(
    val fraisSejourPercentage: Int,
    val chambreParticuliere: BigDecimal?, // Per day
    val accompagnant: BigDecimal?, // Per day
    val forfaitJournalier: Boolean
)
