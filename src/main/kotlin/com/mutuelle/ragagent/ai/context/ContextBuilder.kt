package com.mutuelle.ragagent.ai.context

import com.mutuelle.ragagent.domain.intent.Intent
import com.mutuelle.ragagent.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Builds comprehensive context for an adherent by aggregating data from multiple sources.
 */
@Component
class ContextBuilder(
    private val adherentContextProvider: AdherentContextProvider,
    private val contractContextProvider: ContractContextProvider,
    private val reimbursementContextProvider: ReimbursementContextProvider,
    private val devisContextProvider: DevisContextProvider,
    private val documentContextProvider: DocumentContextProvider
) {
    /**
     * Builds full context for an adherent.
     * Performs parallel retrieval of all data sources.
     */
    suspend fun buildContext(
        adherentId: UUID?,
        intent: Intent? = null
    ): AdherentContext = coroutineScope {
        if (adherentId == null) {
            logger.debug { "No adherent ID provided, returning empty context" }
            return@coroutineScope AdherentContext.empty()
        }

        logger.debug { "Building context for adherent: $adherentId, intent: $intent" }

        // Parallel retrieval of all data sources
        val adherentDeferred = async { adherentContextProvider.provide(adherentId) }
        val contractsDeferred = async { contractContextProvider.provide(adherentId) }
        val reimbursementsDeferred = async {
            if (intent?.requiresAdherentData == true) {
                reimbursementContextProvider.provideRecent(adherentId, limit = 10)
            } else emptyList()
        }
        val consumptionDeferred = async {
            if (intent in listOf(Intent.DEVIS, Intent.REMBOURSEMENT)) {
                reimbursementContextProvider.provideConsumption(adherentId)
            } else emptyList()
        }
        val devisDeferred = async {
            if (intent == Intent.DEVIS) {
                devisContextProvider.provideActive(adherentId)
            } else emptyList()
        }
        val documentsDeferred = async {
            if (intent == Intent.ADMINISTRATIF) {
                documentContextProvider.provideAvailable(adherentId)
            } else emptyList()
        }

        AdherentContext(
            adherent = adherentDeferred.await(),
            contracts = contractsDeferred.await(),
            recentReimbursements = reimbursementsDeferred.await(),
            consumptionTracking = consumptionDeferred.await(),
            activeDevis = devisDeferred.await(),
            availableDocuments = documentsDeferred.await()
        )
    }

    /**
     * Builds lightweight context (only adherent and contracts).
     */
    suspend fun buildLightweightContext(adherentId: UUID?): AdherentContext = coroutineScope {
        if (adherentId == null) {
            return@coroutineScope AdherentContext.empty()
        }

        val adherentDeferred = async { adherentContextProvider.provide(adherentId) }
        val contractsDeferred = async { contractContextProvider.provide(adherentId) }

        AdherentContext(
            adherent = adherentDeferred.await(),
            contracts = contractsDeferred.await()
        )
    }
}

/**
 * Aggregated context for an adherent.
 */
data class AdherentContext(
    val adherent: Adherent? = null,
    val contracts: List<Contract> = emptyList(),
    val recentReimbursements: List<Reimbursement> = emptyList(),
    val consumptionTracking: List<ConsumptionTracker> = emptyList(),
    val activeDevis: List<Devis> = emptyList(),
    val availableDocuments: List<Document> = emptyList()
) {
    companion object {
        fun empty() = AdherentContext()
    }

    val isEmpty: Boolean
        get() = adherent == null

    val activeContract: Contract?
        get() = contracts.find { it.isActive }

    val hasActiveWaitingPeriods: Boolean
        get() = contracts.flatMap { it.waitingPeriods }.any { it.isActive }

    /**
     * Converts context to a human-readable string for LLM consumption.
     */
    fun toContextString(): String = buildString {
        adherent?.let {
            appendLine("=== INFORMATIONS ADHÉRENT ===")
            appendLine("Nom: ${it.fullName}")
            appendLine("Numéro adhérent: ${it.numeroAdherent}")
            appendLine("Email: ${it.email}")
            appendLine()
        }

        if (contracts.isNotEmpty()) {
            appendLine("=== CONTRATS ===")
            contracts.forEach { contract ->
                appendLine("- ${contract.contractNumber}: ${contract.formula.displayFr} (${contract.status.displayFr})")
                appendLine("  Date d'effet: ${contract.startDate}")
                appendLine("  Cotisation: ${contract.monthlyPremium}€/mois")
                if (contract.guarantees.isNotEmpty()) {
                    appendLine("  Garanties principales:")
                    contract.guarantees.take(5).forEach { g ->
                        appendLine("    • ${g.name}: ${g.coverageDisplay}")
                    }
                }
                if (contract.waitingPeriods.any { it.isActive }) {
                    appendLine("  Délais de carence en cours:")
                    contract.waitingPeriods.filter { it.isActive }.forEach { wp ->
                        appendLine("    • ${wp.guaranteeCategory.displayFr}: fin le ${wp.endDate}")
                    }
                }
            }
            appendLine()
        }

        if (recentReimbursements.isNotEmpty()) {
            appendLine("=== REMBOURSEMENTS RÉCENTS ===")
            recentReimbursements.take(5).forEach { r ->
                appendLine("- ${r.careDate}: ${r.careLabel}")
                appendLine("  Statut: ${r.status.displayFr} ${r.status.icon}")
                appendLine("  Montant: ${r.amountClaimed}€ → Remboursé: ${r.totalReimbursed ?: "en cours"}€")
            }
            appendLine()
        }

        if (consumptionTracking.isNotEmpty()) {
            appendLine("=== CONSOMMATION DES PLAFONDS ===")
            consumptionTracking.forEach { ct ->
                appendLine("- ${ct.category.displayFr}: ${ct.consumed}€ / ${ct.ceiling}€ (${ct.consumptionPercentage}%)")
            }
            appendLine()
        }

        if (activeDevis.isNotEmpty()) {
            appendLine("=== DEVIS EN COURS ===")
            activeDevis.forEach { d ->
                appendLine("- ${d.type.displayFr}: ${d.totalEstimated}€ (RAC estimé: ${d.remainingCharge}€)")
                appendLine("  Valide jusqu'au: ${d.validUntil}")
            }
            appendLine()
        }

        if (availableDocuments.isNotEmpty()) {
            appendLine("=== DOCUMENTS DISPONIBLES ===")
            availableDocuments.forEach { doc ->
                appendLine("- ${doc.type.displayFr}: ${doc.title}")
            }
        }
    }
}
