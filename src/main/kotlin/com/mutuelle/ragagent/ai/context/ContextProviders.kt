package com.mutuelle.ragagent.ai.context

import com.mutuelle.ragagent.domain.model.*
import com.mutuelle.ragagent.infrastructure.persistence.repository.*
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Provides adherent profile data.
 */
@Component
class AdherentContextProvider(
    private val adherentRepository: AdherentRepository
) {
    suspend fun provide(adherentId: UUID): Adherent? {
        logger.debug { "Fetching adherent: $adherentId" }
        return adherentRepository.findById(adherentId)
    }

    suspend fun provideByNumero(numeroAdherent: String): Adherent? {
        logger.debug { "Fetching adherent by numero: $numeroAdherent" }
        return adherentRepository.findByNumeroAdherent(numeroAdherent)
    }
}

/**
 * Provides contract and guarantee data.
 */
@Component
class ContractContextProvider(
    private val contractRepository: ContractRepository
) {
    suspend fun provide(adherentId: UUID): List<Contract> {
        logger.debug { "Fetching contracts for adherent: $adherentId" }
        return contractRepository.findByAdherentIdWithGuarantees(adherentId)
    }

    suspend fun provideActive(adherentId: UUID): Contract? {
        logger.debug { "Fetching active contract for adherent: $adherentId" }
        return contractRepository.findActiveByAdherentId(adherentId)
    }
}

/**
 * Provides reimbursement data.
 */
@Component
class ReimbursementContextProvider(
    private val reimbursementRepository: ReimbursementRepository
) {
    suspend fun provideRecent(adherentId: UUID, limit: Int = 10): List<Reimbursement> {
        logger.debug { "Fetching recent reimbursements for adherent: $adherentId, limit: $limit" }
        return reimbursementRepository.findRecentByAdherentId(adherentId, limit)
    }

    suspend fun provideByDateRange(
        adherentId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Reimbursement> {
        logger.debug { "Fetching reimbursements for period: $startDate to $endDate" }
        return reimbursementRepository.findByAdherentIdAndDateRange(adherentId, startDate, endDate)
    }

    suspend fun provideConsumption(adherentId: UUID): List<ConsumptionTracker> {
        logger.debug { "Fetching consumption tracking for adherent: $adherentId" }
        return reimbursementRepository.findConsumptionByAdherentId(adherentId)
    }

    suspend fun providePending(adherentId: UUID): List<Reimbursement> {
        logger.debug { "Fetching pending reimbursements for adherent: $adherentId" }
        return reimbursementRepository.findPendingByAdherentId(adherentId)
    }
}

/**
 * Provides devis (quote) data.
 */
@Component
class DevisContextProvider(
    private val devisRepository: DevisRepository
) {
    suspend fun provideActive(adherentId: UUID): List<Devis> {
        logger.debug { "Fetching active devis for adherent: $adherentId" }
        return devisRepository.findActiveByAdherentId(adherentId)
    }

    suspend fun provideById(devisId: UUID): Devis? {
        logger.debug { "Fetching devis: $devisId" }
        return devisRepository.findByIdWithItems(devisId)
    }
}

/**
 * Provides document data.
 */
@Component
class DocumentContextProvider(
    private val documentRepository: DocumentRepository
) {
    suspend fun provideAvailable(adherentId: UUID): List<Document> {
        logger.debug { "Fetching available documents for adherent: $adherentId" }
        return documentRepository.findValidByAdherentId(adherentId)
    }

    suspend fun provideByType(adherentId: UUID, type: DocumentType): Document? {
        logger.debug { "Fetching document of type $type for adherent: $adherentId" }
        return documentRepository.findLatestByAdherentIdAndType(adherentId, type)
    }
}
