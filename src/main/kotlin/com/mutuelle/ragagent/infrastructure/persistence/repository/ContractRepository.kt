package com.mutuelle.ragagent.infrastructure.persistence.repository

import com.mutuelle.ragagent.domain.model.*
import com.mutuelle.ragagent.infrastructure.persistence.entity.ContractEntity
import com.mutuelle.ragagent.infrastructure.persistence.entity.GuaranteeEntity
import com.mutuelle.ragagent.infrastructure.persistence.entity.WaitingPeriodEntity
import kotlinx.coroutines.flow.toList
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * R2DBC repository for contracts.
 */
@Repository
interface ContractR2dbcRepository : CoroutineCrudRepository<ContractEntity, UUID> {

    suspend fun findByAdherentId(adherentId: UUID): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE adherent_id = :adherentId AND status = 'ACTIVE' LIMIT 1")
    suspend fun findActiveByAdherentId(adherentId: UUID): ContractEntity?

    suspend fun findByContractNumber(contractNumber: String): ContractEntity?
}

/**
 * R2DBC repository for guarantees.
 */
@Repository
interface GuaranteeR2dbcRepository : CoroutineCrudRepository<GuaranteeEntity, UUID> {

    @Query("""
        SELECT g.* FROM guarantees g
        INNER JOIN contract_guarantees cg ON g.id = cg.guarantee_id
        WHERE cg.contract_id = :contractId
    """)
    suspend fun findByContractId(contractId: UUID): List<GuaranteeEntity>

    suspend fun findByCode(code: String): GuaranteeEntity?

    suspend fun findByCategory(category: String): List<GuaranteeEntity>
}

/**
 * R2DBC repository for waiting periods.
 */
@Repository
interface WaitingPeriodR2dbcRepository : CoroutineCrudRepository<WaitingPeriodEntity, UUID> {

    suspend fun findByContractId(contractId: UUID): List<WaitingPeriodEntity>
}

/**
 * High-level repository that maps entities to domain models.
 */
@Repository
class ContractRepository(
    private val contractR2dbcRepository: ContractR2dbcRepository,
    private val guaranteeR2dbcRepository: GuaranteeR2dbcRepository,
    private val waitingPeriodR2dbcRepository: WaitingPeriodR2dbcRepository
) {
    suspend fun findByAdherentIdWithGuarantees(adherentId: UUID): List<Contract> {
        val contracts = contractR2dbcRepository.findByAdherentId(adherentId)
        return contracts.map { entity ->
            val guarantees = guaranteeR2dbcRepository.findByContractId(entity.id!!)
            val waitingPeriods = waitingPeriodR2dbcRepository.findByContractId(entity.id!!)
            entity.toDomain(guarantees, waitingPeriods)
        }
    }

    suspend fun findActiveByAdherentId(adherentId: UUID): Contract? {
        val entity = contractR2dbcRepository.findActiveByAdherentId(adherentId) ?: return null
        val guarantees = guaranteeR2dbcRepository.findByContractId(entity.id!!)
        val waitingPeriods = waitingPeriodR2dbcRepository.findByContractId(entity.id!!)
        return entity.toDomain(guarantees, waitingPeriods)
    }

    suspend fun findByContractNumber(contractNumber: String): Contract? {
        val entity = contractR2dbcRepository.findByContractNumber(contractNumber) ?: return null
        val guarantees = guaranteeR2dbcRepository.findByContractId(entity.id!!)
        val waitingPeriods = waitingPeriodR2dbcRepository.findByContractId(entity.id!!)
        return entity.toDomain(guarantees, waitingPeriods)
    }

    private fun ContractEntity.toDomain(
        guaranteeEntities: List<GuaranteeEntity>,
        waitingPeriodEntities: List<WaitingPeriodEntity>
    ): Contract {
        return Contract(
            id = this.id!!,
            contractNumber = this.contractNumber,
            adherentId = this.adherentId,
            type = ContractType.valueOf(this.type),
            formula = ContractFormula.valueOf(this.formula),
            status = ContractStatus.valueOf(this.status),
            startDate = this.startDate,
            endDate = this.endDate,
            monthlyPremium = this.monthlyPremium,
            guarantees = guaranteeEntities.map { it.toDomain() },
            waitingPeriods = waitingPeriodEntities.map { it.toDomain() },
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun GuaranteeEntity.toDomain(): Guarantee {
        return Guarantee(
            id = this.id!!,
            code = this.code,
            name = this.name,
            description = this.description ?: "",
            category = GuaranteeCategory.valueOf(this.category),
            coveragePercentage = this.coveragePercentage,
            ceiling = this.ceiling,
            frequency = this.frequency?.let { CoverageFrequency.valueOf(it) },
            waitingPeriodDays = this.waitingPeriodDays
        )
    }

    private fun WaitingPeriodEntity.toDomain(): WaitingPeriod {
        return WaitingPeriod(
            guaranteeCategory = GuaranteeCategory.valueOf(this.guaranteeCategory),
            startDate = this.startDate,
            endDate = this.endDate,
            durationMonths = this.durationMonths
        )
    }
}
