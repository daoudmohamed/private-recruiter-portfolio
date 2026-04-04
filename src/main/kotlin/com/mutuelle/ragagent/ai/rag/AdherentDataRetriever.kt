package com.mutuelle.ragagent.ai.rag

import com.mutuelle.ragagent.ai.context.AdherentContext
import com.mutuelle.ragagent.domain.intent.Intent
import com.mutuelle.ragagent.domain.model.*
import mu.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

/**
 * Retriever that generates documents from adherent-specific data.
 * Transforms structured data into text documents for RAG context.
 */
@Component
class AdherentDataRetriever {

    /**
     * Retrieves adherent-specific documents based on intent.
     */
    suspend fun retrieve(
        context: AdherentContext,
        intent: Intent
    ): List<Document> {
        logger.debug { "Retrieving adherent data for intent: $intent" }

        return when (intent) {
            Intent.CONTRAT -> retrieveContractDocuments(context)
            Intent.REMBOURSEMENT -> retrieveReimbursementDocuments(context)
            Intent.DEVIS -> retrieveDevisDocuments(context)
            Intent.ADMINISTRATIF -> retrieveAdministrativeDocuments(context)
            Intent.RECLAMATION -> retrieveReclamationDocuments(context)
            else -> emptyList()
        }
    }

    private fun retrieveContractDocuments(context: AdherentContext): List<Document> {
        val documents = mutableListOf<Document>()

        // Contract summary
        context.contracts.forEach { contract ->
            documents.add(createContractDocument(contract))
        }

        // Active guarantees
        context.contracts.flatMap { it.guarantees }.distinctBy { it.code }.forEach { guarantee ->
            documents.add(createGuaranteeDocument(guarantee))
        }

        // Waiting periods
        context.contracts.flatMap { it.waitingPeriods }.filter { it.isActive }.forEach { waitingPeriod ->
            documents.add(createWaitingPeriodDocument(waitingPeriod))
        }

        return documents
    }

    private fun retrieveReimbursementDocuments(context: AdherentContext): List<Document> {
        val documents = mutableListOf<Document>()

        // Recent reimbursements
        context.recentReimbursements.take(5).forEach { reimbursement ->
            documents.add(createReimbursementDocument(reimbursement))
        }

        // Consumption tracking if available
        context.consumptionTracking.forEach { tracking ->
            documents.add(createConsumptionDocument(tracking))
        }

        return documents
    }

    private fun retrieveDevisDocuments(context: AdherentContext): List<Document> {
        val documents = mutableListOf<Document>()

        // Active quotes
        context.activeDevis.forEach { devis ->
            documents.add(createDevisDocument(devis))
        }

        // Relevant guarantees for simulation
        context.contracts.flatMap { it.guarantees }.forEach { guarantee ->
            documents.add(createGuaranteeDocument(guarantee))
        }

        return documents
    }

    private fun retrieveAdministrativeDocuments(context: AdherentContext): List<Document> {
        val documents = mutableListOf<Document>()

        // Adherent profile
        context.adherent?.let { adherent ->
            documents.add(createAdherentProfileDocument(adherent))
        }

        // Available documents
        context.availableDocuments.forEach { doc ->
            documents.add(createAvailableDocumentInfo(doc))
        }

        return documents
    }

    private fun retrieveReclamationDocuments(context: AdherentContext): List<Document> {
        // For reclamations, include recent interactions and any pending issues
        return retrieveReimbursementDocuments(context) +
                retrieveContractDocuments(context)
    }

    // ==================== Document Creators ====================

    private fun createContractDocument(contract: Contract): Document {
        val content = """
            CONTRAT: ${contract.contractNumber}
            Type: ${contract.type.displayFr}
            Formule: ${contract.formula.displayFr}
            Statut: ${contract.status.displayFr}
            Date d'effet: ${contract.startDate}
            Cotisation mensuelle: ${contract.monthlyPremium}€
            Options actives: ${contract.options.joinToString(", ") { it.displayFr }}
        """.trimIndent()

        return Document(content, mapOf(
            "type" to "contract",
            "contract_number" to contract.contractNumber,
            "source" to "adherent_data"
        ))
    }

    private fun createGuaranteeDocument(guarantee: Guarantee): Document {
        val content = """
            GARANTIE: ${guarantee.name}
            Code: ${guarantee.code}
            Catégorie: ${guarantee.category.displayFr}
            Taux de remboursement: ${guarantee.coveragePercentage}%
            ${guarantee.ceiling?.let { "Plafond: ${it}€" } ?: "Sans plafond"}
            ${guarantee.frequency?.let { "Renouvellement: ${it.displayFr}" } ?: ""}
            ${if (guarantee.waitingPeriodDays > 0) "Délai de carence: ${guarantee.waitingPeriodDays} jours" else "Pas de délai de carence"}
        """.trimIndent()

        return Document(content, mapOf(
            "type" to "guarantee",
            "guarantee_code" to guarantee.code,
            "category" to guarantee.category.name,
            "source" to "adherent_data"
        ))
    }

    private fun createWaitingPeriodDocument(waitingPeriod: WaitingPeriod): Document {
        val content = """
            DÉLAI DE CARENCE EN COURS
            Garantie: ${waitingPeriod.guaranteeCategory.displayFr}
            Fin du délai: ${waitingPeriod.endDate}
            Jours restants: ${waitingPeriod.remainingDays}
        """.trimIndent()

        return Document(content, mapOf(
            "type" to "waiting_period",
            "category" to waitingPeriod.guaranteeCategory.name,
            "source" to "adherent_data"
        ))
    }

    private fun createReimbursementDocument(reimbursement: Reimbursement): Document {
        val content = """
            REMBOURSEMENT: ${reimbursement.careLabel}
            Date des soins: ${reimbursement.careDate}
            Statut: ${reimbursement.status.displayFr} ${reimbursement.status.icon}
            ${reimbursement.beneficiaryName?.let { "Bénéficiaire: $it" } ?: "Titulaire"}
            ${reimbursement.providerName?.let { "Praticien: $it" } ?: ""}
            Montant dépensé: ${reimbursement.amountClaimed}€
            ${reimbursement.secuAmount?.let { "Remboursement Sécu: ${it}€" } ?: ""}
            ${reimbursement.mutuelleAmount?.let { "Remboursement Mutuelle: ${it}€" } ?: ""}
            ${reimbursement.remainingCharge?.let { "Reste à charge: ${it}€" } ?: ""}
            ${reimbursement.paymentDate?.let { "Date de virement: $it" } ?: ""}
        """.trimIndent()

        return Document(content, mapOf(
            "type" to "reimbursement",
            "status" to reimbursement.status.name,
            "care_type" to reimbursement.careType.name,
            "source" to "adherent_data"
        ))
    }

    private fun createConsumptionDocument(tracking: ConsumptionTracker): Document {
        val content = """
            CONSOMMATION ${tracking.category.displayFr}
            Période: ${tracking.periodStart} au ${tracking.periodEnd}
            Plafond: ${tracking.ceiling}€
            Utilisé: ${tracking.consumed}€
            Restant: ${tracking.remaining}€
            Utilisation: ${tracking.consumptionPercentage}%
        """.trimIndent()

        return Document(content, mapOf(
            "type" to "consumption",
            "category" to tracking.category.name,
            "source" to "adherent_data"
        ))
    }

    private fun createDevisDocument(devis: Devis): Document {
        val content = """
            DEVIS ${devis.type.displayFr} ${devis.type.icon}
            Statut: ${devis.status.displayFr}
            Créé le: ${devis.createdAt}
            Valide jusqu'au: ${devis.validUntil}
            ${devis.practitioner?.let { "Praticien: ${it.name}" } ?: ""}

            Montant total estimé: ${devis.totalEstimated}€
            Estimation Sécu: ${devis.secuEstimate}€
            Estimation Mutuelle: ${devis.mutuelleEstimate}€
            Reste à charge estimé: ${devis.remainingCharge}€
            Taux de couverture: ${devis.coveragePercentage}%
        """.trimIndent()

        return Document(content, mapOf(
            "type" to "devis",
            "devis_type" to devis.type.name,
            "status" to devis.status.name,
            "source" to "adherent_data"
        ))
    }

    private fun createAdherentProfileDocument(adherent: Adherent): Document {
        val content = """
            PROFIL ADHÉRENT
            Numéro: ${adherent.numeroAdherent}
            Nom: ${adherent.fullName}
            Email: ${adherent.email}
            Téléphone: ${adherent.phone ?: "Non renseigné"}
            Adresse: ${adherent.address.city}, ${adherent.address.postalCode}
            Préférence de contact: ${adherent.preferenceContact.name}
        """.trimIndent()

        return Document(content, mapOf(
            "type" to "profile",
            "source" to "adherent_data"
        ))
    }

    private fun createAvailableDocumentInfo(document: com.mutuelle.ragagent.domain.model.Document): Document {
        val content = """
            DOCUMENT DISPONIBLE
            Type: ${document.type.displayFr}
            Titre: ${document.title}
            ${document.validUntil?.let { "Valide jusqu'au: $it" } ?: ""}
        """.trimIndent()

        return Document(content, mapOf(
            "type" to "available_document",
            "document_type" to document.type.name,
            "source" to "adherent_data"
        ))
    }
}
