package com.mutuelle.ragagent.infrastructure.persistence.repository

import com.mutuelle.ragagent.domain.model.*
import com.mutuelle.ragagent.infrastructure.persistence.entity.DevisEntity
import com.mutuelle.ragagent.infrastructure.persistence.entity.DevisItemEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * R2DBC repository for devis.
 */
@Repository
interface DevisR2dbcRepository : CoroutineCrudRepository<DevisEntity, UUID> {

    @Query("""
        SELECT * FROM devis
        WHERE adherent_id = :adherentId
        AND status = 'PENDING'
        AND valid_until >= CURRENT_DATE
        ORDER BY created_at DESC
    """)
    suspend fun findActiveByAdherentId(adherentId: UUID): List<DevisEntity>

    @Query("SELECT * FROM devis WHERE adherent_id = :adherentId ORDER BY created_at DESC")
    suspend fun findByAdherentId(adherentId: UUID): List<DevisEntity>
}

/**
 * R2DBC repository for devis items.
 */
@Repository
interface DevisItemR2dbcRepository : CoroutineCrudRepository<DevisItemEntity, UUID> {

    suspend fun findByDevisId(devisId: UUID): List<DevisItemEntity>
}

/**
 * High-level repository for devis.
 */
@Repository
class DevisRepository(
    private val devisR2dbcRepository: DevisR2dbcRepository,
    private val devisItemR2dbcRepository: DevisItemR2dbcRepository
) {
    suspend fun findActiveByAdherentId(adherentId: UUID): List<Devis> {
        return devisR2dbcRepository.findActiveByAdherentId(adherentId)
            .map { entity ->
                val items = devisItemR2dbcRepository.findByDevisId(entity.id!!)
                entity.toDomain(items)
            }
    }

    suspend fun findByIdWithItems(devisId: UUID): Devis? {
        val entity = devisR2dbcRepository.findById(devisId) ?: return null
        val items = devisItemR2dbcRepository.findByDevisId(devisId)
        return entity.toDomain(items)
    }

    suspend fun findByAdherentId(adherentId: UUID): List<Devis> {
        return devisR2dbcRepository.findByAdherentId(adherentId)
            .map { entity ->
                val items = devisItemR2dbcRepository.findByDevisId(entity.id!!)
                entity.toDomain(items)
            }
    }

    private fun DevisEntity.toDomain(itemEntities: List<DevisItemEntity>): Devis {
        return Devis(
            id = this.id!!,
            adherentId = this.adherentId,
            type = DevisType.valueOf(this.type),
            status = DevisStatus.valueOf(this.status),
            createdAt = this.createdAt,
            validUntil = this.validUntil,
            practitioner = if (this.practitionerName != null) {
                PractitionerInfo(
                    name = this.practitionerName!!,
                    specialty = this.practitionerSpecialty,
                    address = this.practitionerAddress,
                    phone = this.practitionerPhone,
                    isPartner = this.practitionerIsPartner
                )
            } else null,
            items = itemEntities.map { it.toDomain() },
            totalEstimated = this.totalEstimated,
            secuEstimate = this.secuEstimate,
            mutuelleEstimate = this.mutuelleEstimate,
            remainingCharge = this.remainingCharge,
            notes = this.notes,
            documentId = this.documentId
        )
    }

    private fun DevisItemEntity.toDomain(): DevisItem {
        return DevisItem(
            id = this.id!!,
            description = this.description,
            codeActe = this.codeActe,
            quantity = this.quantity,
            unitPrice = this.unitPrice,
            secuBase = this.secuBase,
            secuEstimate = this.secuEstimate,
            mutuelleEstimate = this.mutuelleEstimate,
            remainingCharge = this.remainingCharge
        )
    }
}
