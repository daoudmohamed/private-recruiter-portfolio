package com.mutuelle.ragagent.infrastructure.persistence.repository

import com.mutuelle.ragagent.domain.model.*
import com.mutuelle.ragagent.infrastructure.persistence.entity.ConsumptionTrackingEntity
import com.mutuelle.ragagent.infrastructure.persistence.entity.ReimbursementEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

/**
 * R2DBC repository for reimbursements.
 */
@Repository
interface ReimbursementR2dbcRepository : CoroutineCrudRepository<ReimbursementEntity, UUID> {

    @Query("SELECT * FROM reimbursements WHERE adherent_id = :adherentId ORDER BY care_date DESC LIMIT :limit")
    suspend fun findRecentByAdherentId(adherentId: UUID, limit: Int): List<ReimbursementEntity>

    @Query("""
        SELECT * FROM reimbursements
        WHERE adherent_id = :adherentId
        AND care_date BETWEEN :startDate AND :endDate
        ORDER BY care_date DESC
    """)
    suspend fun findByAdherentIdAndDateRange(
        adherentId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ReimbursementEntity>

    @Query("""
        SELECT * FROM reimbursements
        WHERE adherent_id = :adherentId
        AND status IN ('SUBMITTED', 'PROCESSING', 'PENDING_INFO')
        ORDER BY submission_date DESC
    """)
    suspend fun findPendingByAdherentId(adherentId: UUID): List<ReimbursementEntity>

    @Query("SELECT * FROM reimbursements WHERE adherent_id = :adherentId AND status = :status")
    suspend fun findByAdherentIdAndStatus(adherentId: UUID, status: String): List<ReimbursementEntity>
}

/**
 * R2DBC repository for consumption tracking.
 */
@Repository
interface ConsumptionTrackingR2dbcRepository : CoroutineCrudRepository<ConsumptionTrackingEntity, UUID> {

    @Query("""
        SELECT * FROM consumption_tracking
        WHERE adherent_id = :adherentId
        AND period_end >= CURRENT_DATE
    """)
    suspend fun findActiveByAdherentId(adherentId: UUID): List<ConsumptionTrackingEntity>
}

/**
 * High-level repository for reimbursements.
 */
@Repository
class ReimbursementRepository(
    private val reimbursementR2dbcRepository: ReimbursementR2dbcRepository,
    private val consumptionR2dbcRepository: ConsumptionTrackingR2dbcRepository
) {
    suspend fun findRecentByAdherentId(adherentId: UUID, limit: Int): List<Reimbursement> {
        return reimbursementR2dbcRepository.findRecentByAdherentId(adherentId, limit)
            .map { it.toDomain() }
    }

    suspend fun findByAdherentIdAndDateRange(
        adherentId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Reimbursement> {
        return reimbursementR2dbcRepository.findByAdherentIdAndDateRange(adherentId, startDate, endDate)
            .map { it.toDomain() }
    }

    suspend fun findPendingByAdherentId(adherentId: UUID): List<Reimbursement> {
        return reimbursementR2dbcRepository.findPendingByAdherentId(adherentId)
            .map { it.toDomain() }
    }

    suspend fun findConsumptionByAdherentId(adherentId: UUID): List<ConsumptionTracker> {
        return consumptionR2dbcRepository.findActiveByAdherentId(adherentId)
            .map { it.toDomain() }
    }

    suspend fun findById(id: UUID): Reimbursement? {
        return reimbursementR2dbcRepository.findById(id)?.toDomain()
    }

    private fun ReimbursementEntity.toDomain(): Reimbursement {
        return Reimbursement(
            id = this.id!!,
            adherentId = this.adherentId,
            contractId = this.contractId,
            beneficiaryId = this.beneficiaryId,
            beneficiaryName = this.beneficiaryName,
            careDate = this.careDate,
            submissionDate = this.submissionDate,
            processingDate = this.processingDate,
            paymentDate = this.paymentDate,
            status = ReimbursementStatus.valueOf(this.status),
            careType = CareType.valueOf(this.careType),
            careCode = this.careCode,
            careLabel = this.careLabel,
            providerName = this.providerName,
            providerCode = this.providerCode,
            amountClaimed = this.amountClaimed,
            secuBase = this.secuBase,
            secuAmount = this.secuAmount,
            mutuelleAmount = this.mutuelleAmount,
            totalReimbursed = this.totalReimbursed,
            remainingCharge = this.remainingCharge,
            rejectionReason = this.rejectionReason,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun ConsumptionTrackingEntity.toDomain(): ConsumptionTracker {
        return ConsumptionTracker(
            adherentId = this.adherentId,
            category = GuaranteeCategory.valueOf(this.category),
            periodStart = this.periodStart,
            periodEnd = this.periodEnd,
            ceiling = this.ceiling,
            consumed = this.consumed
        )
    }
}
