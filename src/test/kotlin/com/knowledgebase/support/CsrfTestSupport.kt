package com.knowledgebase.support

import org.springframework.test.web.reactive.server.WebTestClient

fun WebTestClient.fetchCsrfToken(): String {
    val result = this.get()
        .uri("/api/v1/recruiter-access/session")
        .exchange()
        .expectStatus().is2xxSuccessful
        .returnResult(ByteArray::class.java)

    return result.responseCookies.getFirst("XSRF-TOKEN")?.value
        ?: error("Missing XSRF-TOKEN cookie in test response")
}
