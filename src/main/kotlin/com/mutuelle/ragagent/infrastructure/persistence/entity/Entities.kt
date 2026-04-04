package com.mutuelle.ragagent.infrastructure.persistence.entity

import com.mutuelle.ragagent.domain.model.Adherent
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * R2DBC entity for adherents table.
 */
@Table("adherents")
data class AdherentEntity(
    @Id val id: UUID? = null,
    @Column("numero_adherent") val numeroAdherent: String,
    val civilite: String,
    @Column("first_name") val firstName: String,
    @Column("last_name") val lastName: String,
    val email: String,
    val phone: String?,
    @Column("date_of_birth") val dateOfBirth: LocalDate,
    val street: String,
    @Column("additional_info") val additionalInfo: String?,
    @Column("postal_code") val postalCode: String,
    val city: String,
    val country: String = "France",
    @Column("preference_contact") val preferenceContact: String = "EMAIL",
    @Column("created_at") val createdAt: Instant = Instant.now(),
    @Column("updated_at") val updatedAt: Instant = Instant.now()
) {
    companion object {
        fun fromDomain(adherent: Adherent): AdherentEntity {
            return AdherentEntity(
                id = adherent.id,
                numeroAdherent = adherent.numeroAdherent,
                civilite = adherent.civilite.name,
                firstName = adherent.firstName,
                lastName = adherent.lastName,
                email = adherent.email,
                phone = adherent.phone,
                dateOfBirth = adherent.dateOfBirth,
                street = adherent.address.street,
                additionalInfo = adherent.address.additionalInfo,
                postalCode = adherent.address.postalCode,
                city = adherent.address.city,
                country = adherent.address.country,
                preferenceContact = adherent.preferenceContact.name,
                createdAt = adherent.createdAt,
                updatedAt = adherent.updatedAt
            )
        }
    }
}

/**
 * R2DBC entity for beneficiaries table.
 */
@Table("beneficiaries")
data class BeneficiaryEntity(
    @Id val id: UUID? = null,
    @Column("adherent_id") val adherentId: UUID,
    @Column("first_name") val firstName: String,
    @Column("last_name") val lastName: String,
    @Column("date_of_birth") val dateOfBirth: LocalDate,
    val relationship: String,
    @Column("social_security_number") val socialSecurityNumber: String?,
    @Column("created_at") val createdAt: Instant = Instant.now()
)

/**
 * R2DBC entity for contracts table.
 */
@Table("contracts")
data class ContractEntity(
    @Id val id: UUID? = null,
    @Column("contract_number") val contractNumber: String,
    @Column("adherent_id") val adherentId: UUID,
    val type: String,
    val formula: String,
    val status: String = "ACTIVE",
    @Column("start_date") val startDate: LocalDate,
    @Column("end_date") val endDate: LocalDate?,
    @Column("monthly_premium") val monthlyPremium: BigDecimal,
    @Column("created_at") val createdAt: Instant = Instant.now(),
    @Column("updated_at") val updatedAt: Instant = Instant.now()
)

/**
 * R2DBC entity for guarantees table.
 */
@Table("guarantees")
data class GuaranteeEntity(
    @Id val id: UUID? = null,
    val code: String,
    val name: String,
    val description: String?,
    val category: String,
    @Column("coverage_percentage") val coveragePercentage: Int,
    val ceiling: BigDecimal?,
    val frequency: String?,
    @Column("waiting_period_days") val waitingPeriodDays: Int = 0
)

/**
 * R2DBC entity for contract_guarantees join table.
 */
@Table("contract_guarantees")
data class ContractGuaranteeEntity(
    @Column("contract_id") val contractId: UUID,
    @Column("guarantee_id") val guaranteeId: UUID,
    @Column("custom_ceiling") val customCeiling: BigDecimal?,
    @Column("custom_percentage") val customPercentage: Int?
)

/**
 * R2DBC entity for waiting_periods table.
 */
@Table("waiting_periods")
data class WaitingPeriodEntity(
    @Id val id: UUID? = null,
    @Column("contract_id") val contractId: UUID,
    @Column("guarantee_category") val guaranteeCategory: String,
    @Column("start_date") val startDate: LocalDate,
    @Column("end_date") val endDate: LocalDate,
    @Column("duration_months") val durationMonths: Int
)

/**
 * R2DBC entity for reimbursements table.
 */
@Table("reimbursements")
data class ReimbursementEntity(
    @Id val id: UUID? = null,
    @Column("adherent_id") val adherentId: UUID,
    @Column("contract_id") val contractId: UUID,
    @Column("beneficiary_id") val beneficiaryId: UUID?,
    @Column("beneficiary_name") val beneficiaryName: String?,
    @Column("care_date") val careDate: LocalDate,
    @Column("submission_date") val submissionDate: LocalDate,
    @Column("processing_date") val processingDate: LocalDate?,
    @Column("payment_date") val paymentDate: LocalDate?,
    val status: String = "SUBMITTED",
    @Column("care_type") val careType: String,
    @Column("care_code") val careCode: String?,
    @Column("care_label") val careLabel: String,
    @Column("provider_name") val providerName: String?,
    @Column("provider_code") val providerCode: String?,
    @Column("amount_claimed") val amountClaimed: BigDecimal,
    @Column("secu_base") val secuBase: BigDecimal?,
    @Column("secu_amount") val secuAmount: BigDecimal?,
    @Column("mutuelle_amount") val mutuelleAmount: BigDecimal?,
    @Column("total_reimbursed") val totalReimbursed: BigDecimal?,
    @Column("remaining_charge") val remainingCharge: BigDecimal?,
    @Column("rejection_reason") val rejectionReason: String?,
    @Column("created_at") val createdAt: Instant = Instant.now(),
    @Column("updated_at") val updatedAt: Instant = Instant.now()
)

/**
 * R2DBC entity for consumption_tracking table.
 */
@Table("consumption_tracking")
data class ConsumptionTrackingEntity(
    @Id val id: UUID? = null,
    @Column("adherent_id") val adherentId: UUID,
    @Column("contract_id") val contractId: UUID,
    val category: String,
    @Column("period_start") val periodStart: LocalDate,
    @Column("period_end") val periodEnd: LocalDate,
    val ceiling: BigDecimal,
    val consumed: BigDecimal = BigDecimal.ZERO,
    @Column("last_updated") val lastUpdated: Instant = Instant.now()
)

/**
 * R2DBC entity for devis table.
 */
@Table("devis")
data class DevisEntity(
    @Id val id: UUID? = null,
    @Column("adherent_id") val adherentId: UUID,
    val type: String,
    val status: String = "PENDING",
    @Column("created_at") val createdAt: Instant = Instant.now(),
    @Column("valid_until") val validUntil: LocalDate,
    @Column("practitioner_name") val practitionerName: String?,
    @Column("practitioner_specialty") val practitionerSpecialty: String?,
    @Column("practitioner_address") val practitionerAddress: String?,
    @Column("practitioner_phone") val practitionerPhone: String?,
    @Column("practitioner_is_partner") val practitionerIsPartner: Boolean = false,
    @Column("total_estimated") val totalEstimated: BigDecimal,
    @Column("secu_estimate") val secuEstimate: BigDecimal,
    @Column("mutuelle_estimate") val mutuelleEstimate: BigDecimal,
    @Column("remaining_charge") val remainingCharge: BigDecimal,
    val notes: String?,
    @Column("document_id") val documentId: UUID?
)

/**
 * R2DBC entity for devis_items table.
 */
@Table("devis_items")
data class DevisItemEntity(
    @Id val id: UUID? = null,
    @Column("devis_id") val devisId: UUID,
    val description: String,
    @Column("code_acte") val codeActe: String?,
    val quantity: Int = 1,
    @Column("unit_price") val unitPrice: BigDecimal,
    @Column("secu_base") val secuBase: BigDecimal?,
    @Column("secu_estimate") val secuEstimate: BigDecimal,
    @Column("mutuelle_estimate") val mutuelleEstimate: BigDecimal,
    @Column("remaining_charge") val remainingCharge: BigDecimal
)

/**
 * R2DBC entity for documents table.
 */
@Table("documents")
data class DocumentEntity(
    @Id val id: UUID? = null,
    @Column("adherent_id") val adherentId: UUID,
    val type: String,
    val title: String,
    val filename: String,
    @Column("content_type") val contentType: String,
    @Column("file_path") val filePath: String,
    @Column("file_size") val fileSize: Long,
    @Column("generated_at") val generatedAt: Instant = Instant.now(),
    @Column("valid_from") val validFrom: LocalDate?,
    @Column("valid_until") val validUntil: LocalDate?
)
