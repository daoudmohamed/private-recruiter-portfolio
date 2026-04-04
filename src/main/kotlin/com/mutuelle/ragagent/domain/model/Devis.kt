package com.mutuelle.ragagent.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Represents a quote (devis) submitted by an adherent.
 */
data class Devis(
    val id: UUID,
    val adherentId: UUID,
    val type: DevisType,
    val status: DevisStatus,
    val createdAt: Instant,
    val validUntil: LocalDate,
    val practitioner: PractitionerInfo? = null,
    val items: List<DevisItem> = emptyList(),
    val totalEstimated: BigDecimal,
    val secuEstimate: BigDecimal,
    val mutuelleEstimate: BigDecimal,
    val remainingCharge: BigDecimal,
    val notes: String? = null,
    val documentId: UUID? = null
) {
    val isExpired: Boolean
        get() = LocalDate.now().isAfter(validUntil)

    val isValid: Boolean
        get() = status == DevisStatus.PENDING && !isExpired

    val coveragePercentage: Int
        get() = if (totalEstimated > BigDecimal.ZERO) {
            (secuEstimate + mutuelleEstimate)
                .divide(totalEstimated, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .toInt()
        } else 0
}

/**
 * Type of quote.
 */
enum class DevisType(val displayFr: String, val icon: String) {
    OPTIQUE("Optique", "👓"),
    DENTAIRE("Dentaire", "🦷"),
    HOSPITALISATION("Hospitalisation", "🏥"),
    APPAREILLAGE("Appareillage", "🦿"),
    AUDIOPROTHESE("Audioprothèse", "👂")
}

/**
 * Status of a quote.
 */
enum class DevisStatus(val displayFr: String) {
    PENDING("En attente"),
    VALIDATED("Validé"),
    REJECTED("Rejeté"),
    EXPIRED("Expiré"),
    USED("Utilisé")
}

/**
 * Line item in a quote.
 */
data class DevisItem(
    val id: UUID,
    val description: String,
    val codeActe: String? = null,
    val quantity: Int = 1,
    val unitPrice: BigDecimal,
    val secuBase: BigDecimal? = null,
    val secuEstimate: BigDecimal,
    val mutuelleEstimate: BigDecimal,
    val remainingCharge: BigDecimal
) {
    val totalPrice: BigDecimal
        get() = unitPrice.multiply(BigDecimal(quantity))

    val totalCoverage: BigDecimal
        get() = secuEstimate + mutuelleEstimate
}

/**
 * Information about the practitioner who provided the quote.
 */
data class PractitionerInfo(
    val name: String,
    val specialty: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val isPartner: Boolean = false
)

/**
 * Simulation request for estimating coverage.
 */
data class CoverageSimulationRequest(
    val adherentId: UUID,
    val type: DevisType,
    val items: List<SimulationItem>
)

data class SimulationItem(
    val description: String,
    val codeActe: String? = null,
    val amount: BigDecimal
)

/**
 * Result of a coverage simulation.
 */
data class CoverageSimulationResult(
    val request: CoverageSimulationRequest,
    val totalAmount: BigDecimal,
    val secuEstimate: BigDecimal,
    val mutuelleEstimate: BigDecimal,
    val remainingCharge: BigDecimal,
    val coverageDetails: List<CoverageDetail>,
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
)

data class CoverageDetail(
    val item: SimulationItem,
    val guarantee: Guarantee?,
    val secuAmount: BigDecimal,
    val mutuelleAmount: BigDecimal,
    val reason: String? = null
)
