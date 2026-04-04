package com.mutuelle.ragagent.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Represents a beneficiary linked to an adherent's contract.
 * Can be a spouse, child, or partner.
 */
data class Beneficiary(
    val id: UUID,
    val adherentId: UUID,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val relationship: BeneficiaryRelationship,
    val socialSecurityNumber: String? = null,
    val createdAt: Instant = Instant.now()
) {
    val fullName: String
        get() = "$firstName $lastName"

    val age: Int
        get() = java.time.Period.between(dateOfBirth, LocalDate.now()).years
}

/**
 * Type of relationship between beneficiary and adherent.
 */
enum class BeneficiaryRelationship(val displayFr: String) {
    SPOUSE("Conjoint(e)"),
    CHILD("Enfant"),
    PARTNER("Partenaire")
}
