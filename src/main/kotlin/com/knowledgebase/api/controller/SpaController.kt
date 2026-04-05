package com.knowledgebase.api.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Serves the React entrypoint for browser routes that should be handled by the
 * SPA client router. WebFlux does not resolve "forward:/index.html" like MVC,
 * so the entrypoint resource is returned directly.
 */
@Controller
class SpaController {

    @GetMapping(
        "/",
        "/access",
        "/access/{path:[^.]*}"
    )
    @ResponseBody
    fun index(): ResponseEntity<Resource> {
        val resource = ClassPathResource("static/index.html")
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .body(resource)
    }
}
