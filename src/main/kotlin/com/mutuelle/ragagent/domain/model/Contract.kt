package com.mutuelle.ragagent.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Represents a health insurance contract.
 */
data class Contract(
    val id: UUID,
    val contractNumber: String,
    val adherentId: UUID,
    val type: ContractType,
    val formula: ContractFormula,
    val status: ContractStatus,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val monthlyPremium: BigDecimal,
    val guarantees: List<Guarantee> = emptyList(),
    val options: List<ContractOption> = emptyList(),
    val waitingPeriods: List<WaitingPeriod> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isActive: Boolean
        get() = status == ContractStatus.ACTIVE

    val annualPremium: BigDecimal
        get() = monthlyPremium.multiply(BigDecimal(12))
}

/**
 * Type of contract.
 */
enum class ContractType(val displayFr: String) {
    INDIVIDUAL("Individuel"),
    FAMILY("Famille"),
    COLLECTIVE("Collectif")
}

/**
 * Contract formula/tier.
 */
enum class ContractFormula(val displayFr: String, val level: Int) {
    ESSENTIELLE("Essentielle", 1),
    CONFORT("Confort", 2),
    PREMIUM("Premium", 3);

    fun isHigherThan(other: ContractFormula): Boolean = this.level > other.level
}

/**
 * Contract status.
 */
enum class ContractStatus(val displayFr: String) {
    ACTIVE("Actif"),
    SUSPENDED("Suspendu"),
    TERMINATED("Résilié"),
    PENDING("En attente")
}

/**
 * Optional add-ons to a contract.
 */
enum class ContractOption(val displayFr: String) {
    ASSISTANCE("Assistance"),
    TELECONSULTATION("Téléconsultation"),
    MEDECINES_DOUCES("Médecines douces"),
    PREVENTION("Prévention"),
    HOSPI_CONFORT("Hospitalisation Confort")
}

/**
 * Waiting period (délai de carence) for a specific guarantee.
 */
data class WaitingPeriod(
    val guaranteeCategory: GuaranteeCategory,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val durationMonths: Int
) {
    val isActive: Boolean
        get() = LocalDate.now().isBefore(endDate)

    val remainingDays: Long
        get() = if (isActive) {
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate)
        } else 0
}
