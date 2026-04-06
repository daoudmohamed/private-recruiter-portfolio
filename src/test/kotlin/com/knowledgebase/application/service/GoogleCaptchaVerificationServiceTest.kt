package com.knowledgebase.application.service

import com.knowledgebase.config.KnowledgeBaseProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class GoogleCaptchaVerificationServiceTest {

    private val recaptchaConfig = KnowledgeBaseProperties.RecruiterAccess.Captcha.Recaptcha(
        secretKey = "secret",
        minimumScore = 0.5,
        action = "request_invitation"
    )

    @Test
    fun `should accept successful recaptcha response with matching action and score`() {
        val response = RecaptchaVerificationResponse(
            success = true,
            score = 0.9,
            action = "request_invitation"
        )

        assertThat(response.isAccepted(recaptchaConfig)).isTrue()
    }

    @Test
    fun `should reject recaptcha response when action does not match`() {
        val response = RecaptchaVerificationResponse(
            success = true,
            score = 0.9,
            action = "other_action"
        )

        assertThat(response.isAccepted(recaptchaConfig)).isFalse()
    }

    @Test
    fun `should reject recaptcha response when score is below threshold`() {
        val response = RecaptchaVerificationResponse(
            success = true,
            score = 0.3,
            action = "request_invitation"
        )

        assertThat(response.isAccepted(recaptchaConfig)).isFalse()
    }

    @Test
    fun `should reject recaptcha response when provider marks verification as unsuccessful`() {
        val response = RecaptchaVerificationResponse(
            success = false,
            score = 0.9,
            action = "request_invitation"
        )

        assertThat(response.isAccepted(recaptchaConfig)).isFalse()
    }

    @Test
    fun `should reject recaptcha response when score is missing`() {
        val response = RecaptchaVerificationResponse(
            success = true,
            score = null,
            action = "request_invitation"
        )

        assertThat(response.isAccepted(recaptchaConfig)).isFalse()
    }

    @Test
    fun `should reject recaptcha response when action is blank`() {
        val response = RecaptchaVerificationResponse(
            success = true,
            score = 0.9,
            action = " "
        )

        assertThat(response.isAccepted(recaptchaConfig)).isFalse()
    }

    @Test
    fun `should bypass verification when captcha provider is none`() {
        val service = GoogleCaptchaVerificationService(
            KnowledgeBaseProperties(
                recruiterAccess = KnowledgeBaseProperties.RecruiterAccess(
                    captcha = KnowledgeBaseProperties.RecruiterAccess.Captcha(
                        provider = KnowledgeBaseProperties.RecruiterAccess.Captcha.Provider.NONE
                    )
                )
            ),
            WebClient.builder()
        )

        assertThat(kotlinx.coroutines.runBlocking { service.verify(null, null) }).isTrue()
    }

    @Test
    fun `should reject blank captcha token when recaptcha v3 is enabled`() {
        val service = GoogleCaptchaVerificationService(
            KnowledgeBaseProperties(
                recruiterAccess = KnowledgeBaseProperties.RecruiterAccess(
                    captcha = KnowledgeBaseProperties.RecruiterAccess.Captcha(
                        provider = KnowledgeBaseProperties.RecruiterAccess.Captcha.Provider.RECAPTCHA_V3,
                        siteKey = "site-key",
                        verifyEnabled = true,
                        recaptcha = recaptchaConfig
                    )
                )
            ),
            WebClient.builder()
        )

        assertThat(kotlinx.coroutines.runBlocking { service.verify("   ", null) }).isFalse()
    }
}
