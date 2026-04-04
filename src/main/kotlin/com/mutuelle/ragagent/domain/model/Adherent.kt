package com.mutuelle.ragagent.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Represents a health insurance member (adhérent).
 */
data class Adherent(
    val id: UUID,
    val numeroAdherent: String,
    val civilite: Civilite,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val dateOfBirth: LocalDate,
    val address: Address,
    val preferenceContact: ContactPreference = ContactPreference.EMAIL,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val fullName: String
        get() = "$firstName $lastName"

    val displayName: String
        get() = "${civilite.display} $lastName"
}

/**
 * Address information for an adherent.
 */
data class Address(
    val street: String,
    val additionalInfo: String? = null,
    val postalCode: String,
    val city: String,
    val country: String = "France"
) {
    val fullAddress: String
        get() = buildString {
            append(street)
            additionalInfo?.let { append("\n$it") }
            append("\n$postalCode $city")
            if (country != "France") append("\n$country")
        }
}

/**
 * Civilité (title) for an adherent.
 */
enum class Civilite(val display: String) {
    M("M."),
    MME("Mme"),
    MLLE("Mlle")
}

/**
 * Preferred contact method for an adherent.
 */
enum class ContactPreference {
    EMAIL,
    SMS,
    COURRIER,
    TELEPHONE
}
