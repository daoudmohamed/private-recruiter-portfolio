package com.knowledgebase.domain.model

import java.time.Instant
import java.util.UUID

data class RecruiterInvitation(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val tokenHash: String = "",
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
    val consumedAt: Instant? = null,
    val status: RecruiterInvitationStatus = RecruiterInvitationStatus.PENDING
)

enum class RecruiterInvitationStatus {
    PENDING,
    CONSUMED,
    EXPIRED
}

data class RecruiterAccessSession(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val invitationId: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant
)
