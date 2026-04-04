package com.mutuelle.ragagent.application.dto

import com.mutuelle.ragagent.domain.intent.Intent
import com.mutuelle.ragagent.domain.model.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * DTO for creating a new session.
 */
data class CreateSessionRequest(
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * DTO for session response.
 */
data class SessionResponse(
    val id: String,
    val adherentId: UUID,
    val status: SessionStatus,
    val startedAt: Instant,
    val lastActivityAt: Instant,
    val messageCount: Int = 0,
    val isEscalated: Boolean = false
)

/**
 * DTO for chat history response.
 */
data class ChatHistoryResponse(
    val sessionId: String,
    val messages: List<ChatMessageDto>
)

data class ChatMessageDto(
    val role: String,
    val content: String,
    val timestamp: Instant
)

/**
 * DTO for document request.
 */
data class DocumentRequest(
    val type: DocumentType,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * DTO for document response.
 */
data class DocumentResponse(
    val id: UUID,
    val type: DocumentType,
    val title: String,
    val downloadUrl: String,
    val validUntil: LocalDate?
)

/**
 * DTO for coverage simulation request.
 */
data class SimulationRequest(
    val type: DevisType,
    val items: List<SimulationItemDto>
)

data class SimulationItemDto(
    val description: String,
    val amount: BigDecimal,
    val codeActe: String? = null
)

/**
 * DTO for coverage simulation response.
 */
data class SimulationResponse(
    val totalAmount: BigDecimal,
    val secuEstimate: BigDecimal,
    val mutuelleEstimate: BigDecimal,
    val remainingCharge: BigDecimal,
    val coveragePercentage: Int,
    val details: List<SimulationDetailDto>,
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
)

data class SimulationDetailDto(
    val description: String,
    val amount: BigDecimal,
    val secuAmount: BigDecimal,
    val mutuelleAmount: BigDecimal,
    val remainingCharge: BigDecimal,
    val guaranteeUsed: String?
)

/**
 * DTO for reimbursement summary.
 */
data class ReimbursementSummaryResponse(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalClaimed: BigDecimal,
    val totalReimbursed: BigDecimal,
    val totalRemainingCharge: BigDecimal,
    val reimbursements: List<ReimbursementDto>
)

data class ReimbursementDto(
    val id: UUID,
    val careDate: LocalDate,
    val careType: String,
    val careLabel: String,
    val status: ReimbursementStatus,
    val amountClaimed: BigDecimal,
    val amountReimbursed: BigDecimal?,
    val remainingCharge: BigDecimal?,
    val beneficiaryName: String?
)

/**
 * DTO for contract summary.
 */
data class ContractSummaryResponse(
    val contractNumber: String,
    val formula: ContractFormula,
    val status: ContractStatus,
    val startDate: LocalDate,
    val monthlyPremium: BigDecimal,
    val guarantees: List<GuaranteeDto>,
    val options: List<String>,
    val activeWaitingPeriods: List<WaitingPeriodDto>
)

data class GuaranteeDto(
    val code: String,
    val name: String,
    val category: String,
    val coveragePercentage: Int,
    val ceiling: BigDecimal?,
    val frequency: String?
)

data class WaitingPeriodDto(
    val category: String,
    val endDate: LocalDate,
    val remainingDays: Long
)

/**
 * DTO for escalation webhook payload.
 */
data class EscalationResolvedPayload(
    val sessionId: String,
    val resolution: String,
    val resolvedBy: String
)

/**
 * DTO for health check response.
 */
data class HealthResponse(
    val status: String,
    val version: String,
    val components: Map<String, ComponentHealth>
)

data class ComponentHealth(
    val status: String,
    val details: Map<String, Any> = emptyMap()
)

/**
 * Extension functions to convert domain models to DTOs.
 */
fun ConversationSession.toResponse(messageCount: Int = 0) = SessionResponse(
    id = this.id,
    adherentId = this.adherentId,
    status = this.status,
    startedAt = this.startedAt,
    lastActivityAt = this.lastActivityAt,
    messageCount = messageCount,
    isEscalated = this.isEscalated
)

fun Guarantee.toDto() = GuaranteeDto(
    code = this.code,
    name = this.name,
    category = this.category.displayFr,
    coveragePercentage = this.coveragePercentage,
    ceiling = this.ceiling,
    frequency = this.frequency?.displayFr
)

fun WaitingPeriod.toDto() = WaitingPeriodDto(
    category = this.guaranteeCategory.displayFr,
    endDate = this.endDate,
    remainingDays = this.remainingDays
)

fun Reimbursement.toDto() = ReimbursementDto(
    id = this.id,
    careDate = this.careDate,
    careType = this.careType.displayFr,
    careLabel = this.careLabel,
    status = this.status,
    amountClaimed = this.amountClaimed,
    amountReimbursed = this.totalReimbursed,
    remainingCharge = this.remainingCharge,
    beneficiaryName = this.beneficiaryName
)
