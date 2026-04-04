package com.knowledgebase.application.service

import com.knowledgebase.config.KnowledgeBaseProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException

class RecruiterInvitationTokenServiceTest {

    private val service = RecruiterInvitationTokenService(
        KnowledgeBaseProperties(
            recruiterAccess = KnowledgeBaseProperties.RecruiterAccess(
                enabled = true,
                tokenSecret = "super-secret"
            )
        )
    )

    @Test
    fun `should create and parse a valid invitation token`() {
        val token = service.createToken("invite-1")
        val parsed = service.parse(token.rawToken)

        assertThat(parsed.invitationId).isEqualTo("invite-1")
        assertThat(parsed.tokenHash).isEqualTo(token.tokenHash)
        assertThat(token.rawToken).doesNotContain("recruteur@example.com")
    }

    @Test
    fun `should reject a malformed invitation token`() {
        val tampered = "invite-1"

        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException::class.java) {
            service.parse(tampered)
        }
    }
}
