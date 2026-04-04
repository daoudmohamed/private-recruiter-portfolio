package com.knowledgebase.application.service

import com.knowledgebase.config.KnowledgeBaseProperties
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations

class InvitationRequestRateLimitServiceTest {

    @Test
    fun `should bypass rate limit in dev profile`() = runBlocking {
        val redisTemplate = mockk<ReactiveRedisTemplate<String, String>>(relaxed = true)
        val valueOperations = mockk<ReactiveValueOperations<String, String>>(relaxed = true)
        val environment = mockk<Environment>()

        every { environment.acceptsProfiles(any<Profiles>()) } returns true
        every { redisTemplate.opsForValue() } returns valueOperations

        val service = InvitationRequestRateLimitService(
            redisTemplate = redisTemplate,
            knowledgeBaseProperties = KnowledgeBaseProperties(),
            environment = environment
        )

        assertThat(service.tryAcquireForIp("127.0.0.1")).isTrue()
        assertThat(service.tryAcquireForEmail("recruteur@example.com")).isTrue()

        coVerify(exactly = 0) { valueOperations.increment(any()) }
    }
}
